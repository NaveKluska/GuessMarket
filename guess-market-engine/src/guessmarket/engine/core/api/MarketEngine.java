package guessmarket.engine.core.api;

import guessmarket.engine.dto.EventDetailsDTO;
import guessmarket.engine.dto.EventSummaryDTO;
import guessmarket.engine.dto.ReceiptDTO;

import java.util.List;

/**
 * The core engine interface for Guess Market.
 * Defines the public API exposed to clients (e.g., Console, JavaFX, Web).
 */
public interface MarketEngine {

    /**
     * Loads the system data from an XML file.
     *
     * @param filePath the path to the XML file containing the events
     * @throws Exception if loading or parsing fails
     */
    void loadData(String filePath) throws Exception;

    /**
     * Retrieves a summary of all events currently loaded in the system.
     *
     * @return a list of EventSummaryDTOs representing all events
     */
    List<EventSummaryDTO> getAllEvents();

    List<EventSummaryDTO> getActiveEvents();

    /**
     * Retrieves detailed status and information for a specific event.
     *
     * @param eventId the unique identifier of the event
     * @return an EventDetailsDTO containing the full details of the event
     */
    EventDetailsDTO getEventDetails(int eventId);

    /**
     * Participates in an event by buying shares of a specific choice.
     * @param memberName  The name of the user making the purchase.
     * @param eventId     The unique identifier of the event.
     * @param optionIndex The index of the option to buy shares for.
     * @param quantity    The number of shares to purchase.
     * @return a ReceiptDTO containing the details of the transaction
     * @throws Exception if the transaction cannot be completed (e.g., invalid input)
     */
    ReceiptDTO buyShares(String memberName, int eventId, int optionIndex, int quantity) throws Exception;

    /**
     * Closes an event and processes the final outcome based on the winning choice.
     *
     * @param eventId            the unique identifier of the event to close
     * @param winningOptionIndex the index of the option that won the event
     * @throws Exception if the event cannot be closed (e.g., event does not exist, or is already closed)
     */
    void closeEvent(int eventId, int winningOptionIndex) throws Exception;

    /**
     * Exits the system and handles any required shutdown or state-saving procedures.
     *
     * @throws Exception if an error occurs during shutdown
     */
    void shutdown() throws Exception;
}
