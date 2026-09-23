package guessmarket.engine.core.trading;

import guessmarket.engine.billing.api.CommissionCalculator;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.User;
import guessmarket.engine.models.orderbook.Fill;
import guessmarket.engine.models.orderbook.Mint;
import guessmarket.engine.models.orderbook.Order;
import guessmarket.engine.models.orderbook.OrderBook;
import guessmarket.engine.models.orderbook.OrderBookEvent;
import guessmarket.engine.models.orderbook.OrderSide;

import java.util.Map;

/**
 * Applies an Order Book match to the balances and holdings it affects: a fill (two users trading
 * directly), a mint (two opposing buys creating new shares), and the commission each one sends to
 * the event's Market Maker.
 * <p>
 * Holds the engine's own live users/eventMarketMakers maps rather than copies - it is the engine's
 * settlement step, built once alongside it, not an independent component with its own state.
 */
public final class OrderBookTradeExecutor {

    private final Map<String, User> users;
    private final Map<String, String> eventMarketMakers;
    private final CommissionCalculator commissionCalculator;

    public OrderBookTradeExecutor(final Map<String, User> users, final Map<String, String> eventMarketMakers, final CommissionCalculator commissionCalculator) {
        this.users = users;
        this.eventMarketMakers = eventMarketMakers;
        this.commissionCalculator = commissionCalculator;
    }

    public void applyFill(final OrderBookEvent obEvent, final int optionIndex, final Fill fill) {
        final boolean restingIsBuy = fill.getRestingOrder().getSide() == OrderSide.BUY;
        final String buyerName = restingIsBuy ? fill.getRestingOrder().getUserName() : fill.getIncomingOrder().getUserName();
        final String sellerName = restingIsBuy ? fill.getIncomingOrder().getUserName() : fill.getRestingOrder().getUserName();

        final double tradeValue = fill.getPrice() * fill.getQuantity();
        final double commission = commissionCalculator.calculate(tradeValue, obEvent.getCommission(), obEvent.getCommissionType(), CommissionType.ON_PURCHASE);

        final User buyer = users.get(buyerName);
        if (buyer != null) {
            buyer.decreaseBalance(tradeValue + commission, "Bought " + fill.getQuantity() + " shares in \"" + obEvent.getName() + "\"");
            if (buyer.getBalance() < 0) {
                buyer.setBlocked(true);
            }
            obEvent.getHoldings().increase(buyerName, optionIndex, fill.getQuantity());
            obEvent.recordSpent(buyerName, tradeValue + commission);
            obEvent.recordSpentOnOption(buyerName, optionIndex, tradeValue);
        }

        final User seller = users.get(sellerName);
        if (seller != null) {
            seller.increaseBalance(tradeValue, "Sold " + fill.getQuantity() + " shares in \"" + obEvent.getName() + "\"");
            obEvent.getHoldings().decrease(sellerName, optionIndex, fill.getQuantity());
            obEvent.recordReceived(sellerName, tradeValue);
        }

        if (commission > 0) {
            obEvent.recordCommissionPaid(buyerName, commission);
        }
        creditCommissionToMm(obEvent, commission);
    }

    /** How many shares of this option the user is already offering across their other still-open
     * asks - what a new sell order must be checked against, since those shares aren't gone yet
     * but are already spoken for. */
    public static int sellReservedQuantity(final OrderBook book, final String userName) {
        int total = 0;
        for (final Order order : book.getSellOrders()) {
            if (order.getUserName().equals(userName)) {
                total += order.getQuantity();
            }
        }
        return total;
    }

    public void applyMint(final OrderBookEvent obEvent, final Mint mint) {
        applyMintSide(obEvent, mint.getOrderA().getUserName(), mint.getOptionIndexA(), mint.getPriceA(), mint.getQuantity());
        applyMintSide(obEvent, mint.getOrderB().getUserName(), mint.getOptionIndexB(), mint.getPriceB(), mint.getQuantity());
    }

    private void applyMintSide(final OrderBookEvent obEvent, final String userName, final int optionIndex, final double price, final int quantity) {
        final double cost = price * quantity;
        final double commission = commissionCalculator.calculate(cost, obEvent.getCommission(), obEvent.getCommissionType(), CommissionType.ON_PURCHASE);

        final User user = users.get(userName);
        if (user != null) {
            user.decreaseBalance(cost + commission, "Minted " + quantity + " shares in \"" + obEvent.getName() + "\"");
            if (user.getBalance() < 0) {
                user.setBlocked(true);
            }
            obEvent.getHoldings().increase(userName, optionIndex, quantity);
            obEvent.recordSpent(userName, cost + commission);
            obEvent.recordSpentOnOption(userName, optionIndex, cost);
        }

        obEvent.increaseAccountBalance(cost);
        if (commission > 0) {
            obEvent.recordCommissionPaid(userName, commission);
        }
        creditCommissionToMm(obEvent, commission);
    }

    private void creditCommissionToMm(final OrderBookEvent obEvent, final double commission) {
        if (commission > 0) {
            final User mm = users.get(eventMarketMakers.get(obEvent.getName()));
            if (mm != null) {
                mm.increaseBalance(commission, "Commission from a trade in \"" + obEvent.getName() + "\"");
                // The ledger behind getProfitOrLoss() already counts every cent the Market Maker
                // SPENDS on this event, so it has to count what the event pays them too. Without
                // this, an MM whose balance never moved (for instance one who only traded with
                // themselves) is reported as having lost exactly the commission they earned.
                obEvent.recordReceived(mm.getName(), commission);
            }
        }
    }
}
