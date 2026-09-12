package guessmarket.engine.util;

import java.util.Locale;

/**
 * Formatting for monetary amounts that appear inside user-facing error messages.
 * The spec requires every decimal shown to the user to carry at most two decimal places,
 * which raw double concatenation does not do (it yields things like 138.62943611198907).
 */
public final class Amounts {

    private Amounts() {
    }

    /**
     * Formats an amount for display with exactly two decimal places.
     * Locale.ENGLISH is pinned deliberately: without it, a machine whose default locale uses a
     * comma decimal separator would render "138,63", and the spec requires English-only output.
     */
    public static String format(final double amount) {
        return String.format(Locale.ENGLISH, "%.2f", amount);
    }
}
