package guessmarket.engine.models.orderbook;

import java.util.HashMap;
import java.util.Map;

public class Holdings
{
    private final Map<String, int[]> byUser = new HashMap<>();
    private final int optionCount;

    public Holdings(final int optionCount)
    {
        this.optionCount = optionCount;
    }

    public int get(final String userName, final int optionIndex)
    {
        final int[] quantities = byUser.get(userName);
        return quantities == null ? 0 : quantities[optionIndex];
    }

    public void increase(final String userName, final int optionIndex, final int amount)
    {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to increase holdings by cannot be negative.");
        }
        byUser.computeIfAbsent(userName, name -> new int[optionCount])[optionIndex] += amount;
    }

    public void decrease(final String userName, final int optionIndex, final int amount)
    {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to decrease holdings by cannot be negative.");
        }
        final int current = get(userName, optionIndex);
        if (current < amount) {
            throw new IllegalArgumentException("User '" + userName + "' only holds " + current + " shares of option " + optionIndex + ", cannot remove " + amount + ".");
        }
        byUser.get(userName)[optionIndex] -= amount;
    }

    public Map<String, Integer> holdersOf(final int optionIndex)
    {
        final Map<String, Integer> result = new HashMap<>();
        for (final Map.Entry<String, int[]> entry : byUser.entrySet()) {
            final int quantity = entry.getValue()[optionIndex];
            if (quantity > 0) {
                result.put(entry.getKey(), quantity);
            }
        }
        return result;
    }
}
