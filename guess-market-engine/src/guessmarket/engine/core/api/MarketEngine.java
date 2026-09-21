package guessmarket.engine.core.api;

import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.UserSummaryDTO;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.orderbook.OrderSide;

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
     * Retrieves a summary of all users currently loaded in the system, including which
     * currently-active events each one is participating in.
     */
    List<UserSummaryDTO> getAllUsers();

    /**
     * Retrieves detailed status and information for a specific event.
     *
     * @param eventName the event's unique name
     * @return an EventDetailsDTO containing the full details of the event
     */
    EventDetailsDTO getEventDetails(String eventName);

    /**
     * Participates in an event by buying shares of a specific choice.
     * @param memberName  The name of the user making the purchase.
     * @param eventName   The event's unique name.
     * @param optionIndex The index of the option to buy shares for.
     * @param quantity    The number of shares to purchase.
     * @return a ReceiptDTO containing the details of the transaction
     * @throws Exception if the transaction cannot be completed (e.g., invalid input)
     */
    ReceiptDTO buyShares(String memberName, String eventName, int optionIndex, int quantity) throws Exception;

    /**
     * Opens an event for trading. Only the user assigned as the event's Market Maker may open it,
     * and only from NOT_ACTIVE status. For LMSR events this pays the initial subsidy from the
     * Market Maker's balance into the event's account.
     *
     * @param mmName    the name of the user attempting to open the event (must be its assigned MM)
     * @param eventName the unique name of the event to open
     * @throws Exception if the event cannot be opened (wrong MM, wrong status, insufficient funds, etc.)
     */
    void openEvent(String mmName, String eventName) throws Exception;

    /**
     * Closes an event and processes the final outcome based on the winning choice.
     * Only the user assigned as the event's Market Maker may close it, and only from ACTIVE status.
     *
     * @param mmName             the name of the user attempting to close the event (must be its assigned MM)
     * @param eventName          the unique name of the event to close
     * @param winningOptionIndex the index of the option that won the event
     * @throws Exception if the event cannot be closed (wrong MM, wrong status, event does not exist, etc.)
     */
    void closeEvent(String mmName, String eventName, int winningOptionIndex) throws Exception;

    /**
     * Submits a buy or sell order for an Order Book event's option. May fully or partially match
     * against resting orders, trigger a mint against the opposite option, or simply rest.
     *
     * @param userName    the user submitting the order
     * @param eventName   the Order Book event to trade on
     * @param optionIndex the option to trade
     * @param side        BUY or SELL
     * @param price       the limit price per share
     * @param quantity    the number of shares
     * @throws Exception if the order cannot be submitted (wrong event type, not active, insufficient holdings to sell, etc.)
     */
    void submitOrder(String userName, String eventName, int optionIndex, OrderSide side, double price, int quantity) throws Exception;

    /**
     * Creates a brand-new event from scratch and makes the creating user its Market Maker.
     * The event starts NOT_ACTIVE like any loaded one, so the creator still has to open it (and
     * pay the subsidy or initial allocation) before trading can happen.
     * <p>
     * Inputs are validated to the same rules the file parser applies, so an event created here
     * cannot be less valid than one loaded from XML.
     *
     * @param creatorName    the user creating the event, who becomes its Market Maker
     * @param name           the event's display name
     * @param description    the event's description
     * @param commission     the commission percentage
     * @param commissionType whether commission is taken on purchase or on close
     * @param optionNames    the option names - exactly two, non-empty and distinct
     * @param method         the trading method and its settings
     * @return the new event's name
     * @throws Exception if the creator is unknown or blocked, the name is already taken, or any detail is invalid
     */
    String createEvent(String creatorName, String name, String description, int commission,
                    CommissionType commissionType, List<String> optionNames,
                    MarketMethodSpec method) throws Exception;

    /**
     * Saves the current system state to an external file.
     *
     * @param filePath the full path (without extension) to save the state to
     * @throws Exception if saving fails
     */
    void saveState(String filePath) throws Exception;

    /**
     * Loads a previously saved system state from a file.
     *
     * @param filePath the full path (without extension) to load the state from
     * @throws Exception if loading fails
     */
    void loadState(String filePath) throws Exception;

    /**
     * Exits the system and handles any required shutdown or state-saving procedures.
     *
     * @throws Exception if an error occurs during shutdown
     */
    void shutdown() throws Exception;
}
