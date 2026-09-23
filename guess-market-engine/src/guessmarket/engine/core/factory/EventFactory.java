package guessmarket.engine.core.factory;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Option;
import guessmarket.engine.models.lmsr.LmsrEvent;
import guessmarket.engine.models.orderbook.OrderBookEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates and builds the pieces a brand-new Event needs, whether it came from createEvent's
 * raw arguments or (in principle) any other caller with the same shape of input. Stateless - the
 * same validation rules the file parser applies, so a created event can never be less valid than
 * a loaded one.
 */
public final class EventFactory {

    private EventFactory() {
    }

    /** Validates and builds the option list: exactly two, named, distinct. */
    public static List<Option> buildOptions(final List<String> optionNames) {
        if (optionNames == null || optionNames.size() != 2) {
            throw new IllegalArgumentException("An event must have exactly two options.");
        }
        final List<Option> options = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final String raw : optionNames) {
            final String optionName = raw == null ? "" : raw.trim();
            if (optionName.isEmpty()) {
                throw new IllegalArgumentException("Every option must have a name.");
            }
            if (!seen.add(optionName.toLowerCase())) {
                throw new IllegalArgumentException("Duplicate option name found ('" + optionName + "').");
            }
            options.add(new Option(optionName));
        }
        return options;
    }

    /** Builds a validated LMSR event. */
    public static LmsrEvent buildLmsrEvent(final String name, final String description, final int commission, final CommissionType commissionType, final List<Option> options, final int b) {
        if (b <= 0) {
            throw new IllegalArgumentException("LMSR 'b' must be a positive integer (got " + b + ").");
        }
        return new LmsrEvent(name, description, commission, commissionType, options, b);
    }

    /** Builds a validated Order Book event. */
    public static OrderBookEvent buildOrderBookEvent(final String name, final String description, final int commission, final CommissionType commissionType, final List<Option> options, final boolean allowMint, final int initial, final int d) {
        if (initial < 0) {
            throw new IllegalArgumentException("Order Book 'initial' must not be negative (got " + initial + ").");
        }
        if (d <= 0) {
            throw new IllegalArgumentException("Order Book 'd' (base value) must be a positive integer (got " + d + ").");
        }
        return new OrderBookEvent(name, description, commission, commissionType, options, allowMint, initial, d);
    }
}
