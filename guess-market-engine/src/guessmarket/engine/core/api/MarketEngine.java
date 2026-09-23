package guessmarket.engine.core.api;

import guessmarket.dto.ChatMessageDTO;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.UserSummaryDTO;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.orderbook.OrderSide;

import java.io.InputStream;
import java.util.List;

/**
 * The core engine interface for Guess Market.
 * Defines the public API exposed to clients (e.g., Console, JavaFX, Web).
 */
public interface MarketEngine {

    /**
     * Retrieves a summary of all events currently loaded in the system.
     *
     * @return a list of EventSummaryDTOs representing all events
     */
    List<EventSummaryDTO> getAllEvents();

    /**
     * Parses an uploaded XML file and adds its events to the pool - on top of what is already
     * there, never replacing it. The uploader becomes the Market Maker of every event in the file.
     *
     * @param uploaderName the user who uploaded the file, who becomes MM of every event in it
     * @param xml          the raw file content, unread - encoding is handled by the parser itself
     * @throws Exception if the uploader is unknown, the file is invalid, or any event's name is
     *                    already taken by an event from an earlier upload
     */
    void addEventsFromUpload(String uploaderName, InputStream xml) throws Exception;

    /**
     * Retrieves a summary of all users currently loaded in the system, including which
     * currently-active events each one is participating in.
     */
    List<UserSummaryDTO> getAllUsers();

    /**
     * Registers a brand-new user under this name, with a starting balance of zero. Called once,
     * the first time someone logs in - there is no separate "create account" step.
     *
     * @param name the username to register - must not already be taken (case-insensitively)
     * @throws Exception if the name is empty or already taken
     */
    void registerUser(String name) throws Exception;

    /**
     * Adds funds to an existing user's balance. The only way a user's balance can ever increase
     * without trading - everyone starts at zero, so this is how money enters the system at all.
     *
     * @param userName the user depositing funds
     * @param amount   the amount to add - must be positive
     * @throws Exception if the user is unknown or the amount is not positive
     */
    void depositCash(String userName, double amount) throws Exception;

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
     * Creates a brand-new LMSR event from scratch and makes the creating user its Market Maker.
     * The event starts NOT_ACTIVE like any loaded one, so the creator still has to open it (and
     * pay the initial subsidy) before trading can happen.
     * <p>
     * Inputs are validated to the same rules the file parser applies, so an event created here
     * cannot be less valid than one loaded from XML. The name must already be trimmed - it is the
     * key every later call (openEvent, closeEvent, buyShares...) uses to find this event again, so
     * there is no silent whitespace-trimming here to accidentally drift from what the caller has.
     *
     * @param creatorName    the user creating the event, who becomes its Market Maker
     * @param name           the event's display name - must not be empty or padded with whitespace
     * @param description    the event's description
     * @param commission     the commission percentage
     * @param commissionType whether commission is taken on purchase or on close
     * @param optionNames    the option names - exactly two, non-empty and distinct
     * @param b              the LMSR liquidity parameter - must be positive
     * @throws Exception if the creator is unknown or blocked, the name is invalid or already taken, or any detail is invalid
     */
    void createLmsrEvent(String creatorName, String name, String description, int commission, CommissionType commissionType, List<String> optionNames, int b) throws Exception;

    /**
     * Creates a brand-new Order Book event from scratch and makes the creating user its Market Maker.
     * The event starts NOT_ACTIVE like any loaded one, so the creator still has to open it (and
     * pay for the initial share allocation) before trading can happen.
     * <p>
     * Inputs are validated to the same rules the file parser applies, so an event created here
     * cannot be less valid than one loaded from XML. The name must already be trimmed - see
     * {@link #createLmsrEvent} for why.
     *
     * @param creatorName    the user creating the event, who becomes its Market Maker
     * @param name           the event's display name - must not be empty or padded with whitespace
     * @param description    the event's description
     * @param commission     the commission percentage
     * @param commissionType whether commission is taken on purchase or on close
     * @param optionNames    the option names - exactly two, non-empty and distinct
     * @param allowMint      whether two opposing buy orders whose prices together reach d may mint new shares
     * @param initial        how many shares of every option the Market Maker buys when opening the event
     * @param d              the base value a winning share pays out at close - must be positive
     * @throws Exception if the creator is unknown or blocked, the name is invalid or already taken, or any detail is invalid
     */
    void createOrderBookEvent(String creatorName, String name, String description, int commission, CommissionType commissionType, List<String> optionNames, boolean allowMint, int initial, int d) throws Exception;

    /**
     * Posts a chat message, visible to every connected user.
     *
     * @param userName the user posting the message - must be a registered user
     * @param message  the message text - must not be empty
     * @throws Exception if the user is unknown or the message is empty
     */
    void postChatMessage(String userName, String message) throws Exception;

    /**
     * Returns the full chat log, oldest first - polled the same way events/users already are.
     */
    List<ChatMessageDTO> getChatMessages();
}
