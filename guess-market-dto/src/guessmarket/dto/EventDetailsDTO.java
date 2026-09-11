package guessmarket.dto;

/**
 * Common shape shared by both event types. LMSR and Order Book events show fundamentally
 * different data beyond this (trade history vs. order book + quotes), so each gets its own
 * implementation - see guessmarket.dto.lmsr.LmsrEventDetailsDTO and
 * guessmarket.dto.orderbook.OrderBookEventDetailsDTO.
 */
public interface EventDetailsDTO
{
    int getId();

    String getName();

    String getDescription();

    int getCommission();

    String getCommissionType();

    String getStatus();

    String getType();

    double getAccountBalance();

    String getWinningOptionName();
}
