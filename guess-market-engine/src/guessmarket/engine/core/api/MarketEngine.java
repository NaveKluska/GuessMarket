package guessmarket.engine.core.api;

import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.UserDTO;

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
     * Retrieves all users loaded in the system.
     *
     * @return a list of UserDTOs
     */
    List<UserDTO> getAllUsers();

    /**
     * Retrieves detailed status and information for a specific event.
     *
     * @param eventId the unique identifier of the event
     * @return an EventDetailsDTO containing the full details of the event
     */
    EventDetailsDTO getEventDetails(int eventId);

    /**
     * Starts an event, paying initial subsidy or buying initial shares.
     */
    void startEvent(int eventId, String userName) throws Exception;

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
     * @param userName           the name of the user attempting to close it (must be MM)
     * @throws Exception if the event cannot be closed (e.g., event does not exist, or is already closed)
     */
    void closeEvent(int eventId, int winningOptionIndex, String userName) throws Exception;

    /**
     * Saves the current system state to an external file.
     *
     * @param filePath the full path (without extension) to save the state to
     * @throws Exception if saving fails
     */
    void saveState(String filePath) throws Exception;

    /**
     * Creates a new LMSR event and adds it to the system.
     */
    int createLmsrEvent(String name, String description, int commission, guessmarket.engine.models.CommissionType commissionType, java.util.List<String> optionNames, int b);

    /**
     * Creates a new Order Book event and adds it to the system.
     */
    int createOrderBookEvent(String name, String description, int commission, guessmarket.engine.models.CommissionType commissionType, java.util.List<String> optionNames, boolean allowMint, int initial, int d);

    /**
     * Places a limit order in an Order Book event.
     */
    void placeOrder(String userName, int eventId, int optionIndex, int quantity, double price, guessmarket.engine.models.Order.Type type) throws Exception;

    /**
     * Returns all pending (unmatched) orders for an Order Book event.
     */
    java.util.List<guessmarket.dto.PendingOrderDTO> getPendingOrders(int eventId);

    void loadState(String filePath) throws Exception;

    /**
     * Adds funds to a user's account balance.
     *
     * @param userName the name of the user.
     * @param amount the amount to add (must be positive).
     */
    void addFunds(String userName, double amount);

    /**
     * Exits the system and handles any required shutdown or state-saving procedures.
     *
     * @throws Exception if an error occurs during shutdown
     */
    void shutdown() throws Exception;
}
