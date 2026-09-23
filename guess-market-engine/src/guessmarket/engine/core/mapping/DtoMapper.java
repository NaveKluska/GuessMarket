package guessmarket.engine.core.mapping;

import guessmarket.dto.AccountEntryDTO;
import guessmarket.dto.ChartPointDTO;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.TransactionDTO;
import guessmarket.dto.lmsr.LmsrEventDetailsDTO;
import guessmarket.dto.orderbook.OptionBookDTO;
import guessmarket.dto.orderbook.OrderBookEventDetailsDTO;
import guessmarket.dto.orderbook.OrderDTO;
import guessmarket.dto.orderbook.ParticipantHoldingDTO;
import guessmarket.engine.models.AccountEntry;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.EventStatus;
import guessmarket.engine.models.Option;
import guessmarket.engine.models.Transaction;
import guessmarket.engine.models.User;
import guessmarket.engine.models.lmsr.LmsrEvent;
import guessmarket.engine.models.orderbook.MarketQuote;
import guessmarket.engine.models.orderbook.Order;
import guessmarket.engine.models.orderbook.OrderBookEvent;
import guessmarket.engine.models.orderbook.PricePoint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts engine model objects (Event, User) into the DTOs the API layer sends out.
 * Stateless - every piece of information a conversion needs (such as an event's Market Maker
 * name) is passed in rather than looked up, so this class has nothing of its own to hold.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static EventSummaryDTO mapToSummaryDTO(final Event event, final String mmName) {
        final List<String> optionNames = new ArrayList<>();
        for (final Option option : event.getOptions()) {
            optionNames.add(option.getName());
        }

        return new EventSummaryDTO(
            event.getName(),
            event.getDescription(),
            event.getCommission(),
            event.getCommissionType().name(),
            optionNames,
            event.getStatus().name(),
            (event instanceof LmsrEvent) ? "LMSR" : "ORDER_BOOK",
            mmName,
            event.getAccountBalance()
        );
    }

    public static EventDetailsDTO mapToDetailsDTO(final Event event, final String mmName) {
        if (event instanceof LmsrEvent) {
            return mapLmsrDetailsDTO(event, mmName);
        }
        return mapOrderBookDetailsDTO((OrderBookEvent) event, mmName);
    }

    private static LmsrEventDetailsDTO mapLmsrDetailsDTO(final Event event, final String mmName) {
        final List<Option> options = event.getOptions();
        final List<List<ChartPointDTO>> priceHistories = lmsrPriceHistories((LmsrEvent) event);

        final List<OptionDTO> optionDTOs = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            final Option option = options.get(i);
            optionDTOs.add(new OptionDTO(option.getName(), option.getSharesBought(), event.getOptionProbability(i), priceHistories.get(i)));
        }

        final List<TransactionDTO> transactionDTOs = new ArrayList<>();
        for (final Transaction tx : event.getTransactions()) {
            transactionDTOs.add(new TransactionDTO(tx.getUserName(), tx.getOptionName(), tx.getQuantity(), tx.getPricePaid(), tx.getCommissionPaid(), tx.getTimestamp()));
        }

        return new LmsrEventDetailsDTO(
            event.getName(), event.getDescription(), event.getCommission(), event.getCommissionType().name(),
            event.getStatus().name(), event.getAccountBalance(), event.getTotalCommissionCollected(),
            optionDTOs, transactionDTOs, event.getWinningOptionName(), event.getCommissionPaidByUserMap(),
            mmName
        );
    }

    private static OrderBookEventDetailsDTO mapOrderBookDetailsDTO(final OrderBookEvent event, final String mmName) {
        final List<OptionBookDTO> optionBooks = new ArrayList<>();
        final List<Option> options = event.getOptions();
        for (int i = 0; i < options.size(); i++) {
            final Option option = options.get(i);
            final MarketQuote quote = event.getQuote(i);
            final List<OrderDTO> bids = toOrderDTOs(event.getMarket().getBook(i).bestBuyOrdersFirst());
            final List<OrderDTO> asks = toOrderDTOs(event.getMarket().getBook(i).bestSellOrdersFirst());
            optionBooks.add(new OptionBookDTO(option.getName(), quote.getLast(), quote.getBid(), quote.getAsk(), quote.getMid(), quote.getSpread(), bids, asks, toChartPoints(event.getMarket().getBook(i).getPriceHistory())));
        }

        final boolean closed = event.getStatus() == EventStatus.CLOSED;
        final List<ParticipantHoldingDTO> participants = new ArrayList<>();
        for (final String participantName : event.getParticipants()) {
            final List<Integer> holdings = new ArrayList<>();
            final List<Double> paidByOption = new ArrayList<>();
            double estimatedValue = 0.0;
            for (int i = 0; i < options.size(); i++) {
                final int quantity = event.getHoldings().get(participantName, i);
                holdings.add(quantity);
                paidByOption.add(event.getSpentOnOption(participantName, i));
                final MarketQuote quote = event.getQuote(i);
                final Double priceEstimate = quote.getMid() != null ? quote.getMid() : quote.getLast();
                estimatedValue += quantity * (priceEstimate != null ? priceEstimate : event.getD() / 2.0);
            }
            final double commissionPaid = event.getCommissionPaidBy(participantName);
            final Double profitOrLoss = closed ? event.getProfitOrLoss(participantName) : null;
            participants.add(new ParticipantHoldingDTO(participantName, holdings, paidByOption, estimatedValue, commissionPaid, profitOrLoss));
        }

        return new OrderBookEventDetailsDTO(
            event.getName(), event.getDescription(), event.getCommission(), event.getCommissionType().name(),
            event.getStatus().name(), event.getAccountBalance(), event.getD(), event.isAllowMint(),
            optionBooks, participants, event.getWinningOptionName(),
            mmName
        );
    }

    /**
     * Reconstructs each LMSR option's price after every trade, for the price-over-time chart.
     * <p>
     * An LMSR price depends only on how many shares of each option have been bought, so replaying
     * the event's transactions in order and re-asking the event for its prices at each step
     * reproduces the whole history exactly - there is no need to have stored it as trading happened.
     * The shares are restored afterwards, so this read-only query leaves the event untouched.
     *
     * @return one list of points per option, in option order, each starting at the opening price
     */
    private static List<List<ChartPointDTO>> lmsrPriceHistories(final LmsrEvent event) {
        final List<Option> options = event.getOptions();
        final List<List<ChartPointDTO>> histories = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            histories.add(new ArrayList<>());
        }

        // Replayed in a local array and fed through the event's pure price function, so nothing in
        // the live event is touched by what is only a read.
        final int[] shares = new int[options.size()];
        final List<Transaction> transactions = event.getTransactions();

        // The opening price of every option, before anyone traded.
        final LocalDateTime start = transactions.isEmpty() ? LocalDateTime.now() : transactions.get(0).getTimestamp();
        for (int i = 0; i < options.size(); i++) {
            histories.get(i).add(new ChartPointDTO(start, event.probabilityAt(shares, i)));
        }

        for (final Transaction tx : transactions) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getName().equals(tx.getOptionName())) {
                    shares[i] += tx.getQuantity();
                }
            }
            for (int i = 0; i < options.size(); i++) {
                histories.get(i).add(new ChartPointDTO(tx.getTimestamp(), event.probabilityAt(shares, i)));
            }
        }
        return histories;
    }

    /** Converts the order book's recorded traded prices into plottable points. */
    private static List<ChartPointDTO> toChartPoints(final List<PricePoint> points) {
        final List<ChartPointDTO> result = new ArrayList<>();
        for (final PricePoint point : points) {
            result.add(new ChartPointDTO(point.at(), point.price()));
        }
        return result;
    }

    private static List<OrderDTO> toOrderDTOs(final List<Order> orders) {
        final List<OrderDTO> result = new ArrayList<>();
        for (final Order order : orders) {
            result.add(new OrderDTO(order.getUserName(), order.getQuantity(), order.getPrice()));
        }
        return result;
    }

    /** Converts a user's account history into plottable balance-over-time points, for the chart. */
    public static List<ChartPointDTO> balanceHistoryOf(final User user) {
        final List<ChartPointDTO> points = new ArrayList<>();
        for (final AccountEntry entry : user.getAccountHistory()) {
            points.add(new ChartPointDTO(entry.at(), entry.balanceAfter()));
        }
        return points;
    }

    /** Converts a user's account history into the full, labeled row-by-row log. */
    public static List<AccountEntryDTO> accountHistoryOf(final User user) {
        final List<AccountEntryDTO> entries = new ArrayList<>();
        for (final AccountEntry entry : user.getAccountHistory()) {
            entries.add(new AccountEntryDTO(entry.at(), entry.description(), entry.amount(), entry.balanceAfter()));
        }
        return entries;
    }
}
