package guessmarket.engine.core.settlement;

import guessmarket.engine.billing.api.CommissionCalculator;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.User;

import java.util.Map;

/**
 * Pays out every winning holder once an event closes, taking the close-time commission out of
 * each payout and crediting it to the Market Maker.
 * <p>
 * Holds the engine's own live users map rather than a copy - it is the engine's own close-time
 * settlement step, built once alongside it, not an independent component with its own state.
 */
public final class EventCloser {

    private final Map<String, User> users;
    private final CommissionCalculator commissionCalculator;

    public EventCloser(final Map<String, User> users, final CommissionCalculator commissionCalculator) {
        this.users = users;
        this.commissionCalculator = commissionCalculator;
    }

    /** Pays out every winning holder of this event - the event itself knows who won what and how much; this only knows how to turn that into money. */
    public void closeEvent(final Event event, final int winningOptionIndex, final User mm) {
        for (final Map.Entry<String, Double> payout : event.winningPayouts(winningOptionIndex)) {
            payOutWinner(event, payout.getKey(), payout.getValue(), mm);
        }
    }

    private void payOutWinner(final Event event, final String winnerName, final double winAmount, final User mm) {
        final double closeCommission = commissionCalculator.calculate(winAmount, event.getCommission(), event.getCommissionType(), CommissionType.ON_CLOSE);
        final double payout = winAmount - closeCommission;

        event.decreaseAccountBalance(winAmount);
        if (closeCommission > 0) {
            event.collectCommission(closeCommission);
            event.recordCommissionPaid(winnerName, closeCommission);
            mm.increaseBalance(closeCommission, "Close commission from \"" + event.getName() + "\"");
            // Close-time commission is money the event pays the Market Maker, so the profit/loss
            // ledger has to see it - a no-op for event types that don't keep one.
            event.recordPayoutReceived(mm.getName(), closeCommission);
        }

        final User winner = users.get(winnerName);
        if (winner != null) {
            winner.increaseBalance(payout, "Payout from \"" + event.getName() + "\"");
        }
        event.recordPayoutReceived(winnerName, payout);
    }
}
