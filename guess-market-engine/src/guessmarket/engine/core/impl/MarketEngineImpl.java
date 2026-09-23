package guessmarket.engine.core.impl;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.core.factory.EventFactory;
import guessmarket.engine.core.mapping.DtoMapper;
import guessmarket.engine.core.settlement.EventCloser;
import guessmarket.engine.core.trading.OrderBookTradeExecutor;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.UserSummaryDTO;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.EventStatus;
import guessmarket.engine.models.orderbook.Fill;
import guessmarket.engine.models.orderbook.Mint;
import guessmarket.engine.models.orderbook.OrderBookEvent;
import guessmarket.engine.models.orderbook.OrderSide;
import guessmarket.engine.models.orderbook.TradeOutcome;
import guessmarket.engine.models.Option;
import guessmarket.engine.models.User;
import guessmarket.engine.billing.api.CommissionCalculator;
import guessmarket.engine.parsing.api.FileParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ============================================================================
// DECISIONS MADE
// ============================================================================
// - Names (usernames, event names) are rejected outright if empty or padded
//   with whitespace, never silently trimmed - they're lookup keys, and a
//   trimmed copy the caller doesn't know about would break later lookups.
// - Usernames are unique case-insensitively; event names are compared exactly.
// - A user/Market Maker is only blocked from acting if ALREADY blocked - any
//   single legal action (buying shares, opening an event) may push them
//   negative for the first time, at which point they're blocked afterward.
//   Recovery is only via depositCash.
// - Thread-safety: every operation on a given Event (a trade, an open, a
//   close, even a read for a DTO) is synchronized on that Event object for
//   its full duration - not just the write - so two concurrent operations on
//   the SAME event can never interleave. users/events are separately locked,
//   only around their own membership checks (register/create).
// - Event creation is two methods (createLmsrEvent/createOrderBookEvent), not
//   one method with a variant-type parameter - see EventFactory.
// - addEventsFromUpload commits all-or-nothing: every event name is checked
//   against the pool before any of them are written.
// - closeEvent never checks isBlocked() - closing is how a blocked MM's event
//   gets resolved and everyone paid out; blocking it would strand the funds.
// - submitOrder's SELL check only verifies shares exist and aren't already
//   reserved by another open ask - holdings don't move until a real fill.
// - EventCreator validation skips name-uniqueness - that check only matters
//   together with the write, so it happens atomically in registerCreatedEvent.
// ============================================================================

public class MarketEngineImpl implements MarketEngine
{
    private final Map<String, Event> events;
    private final Map<String, User> users;
    private final Map<String, String> eventMarketMakers;
    private final FileParser parser;
    private final CommissionCalculator commissionCalculator;
    private final OrderBookTradeExecutor orderBookTradeExecutor;
    private final EventCloser eventCloser;

    public MarketEngineImpl(final FileParser parser, final CommissionCalculator commissionCalculator)
    {
        this.parser = parser;
        this.commissionCalculator = commissionCalculator;
        this.events = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
        this.eventMarketMakers = new ConcurrentHashMap<>();
        this.orderBookTradeExecutor = new OrderBookTradeExecutor(this.users, this.eventMarketMakers, this.commissionCalculator);
        this.eventCloser = new EventCloser(this.users, this.commissionCalculator);
    }

    @Override
    public List<EventSummaryDTO> getAllEvents()
    {
        final List<EventSummaryDTO> result = new ArrayList<>();

        for (final Event event : this.events.values())
        {
            synchronized (event) {
                result.add(DtoMapper.mapToSummaryDTO(event, eventMarketMakers.get(event.getName())));
            }
        }

        return result;
    }

    @Override
    public void addEventsFromUpload(final String uploaderName, final InputStream xml) throws Exception
    {
        final User uploader = requireUser(uploaderName);
        final List<Event> parsedEvents = parser.parse(xml);

        synchronized (events) {
            for (final Event event : parsedEvents) {
                if (events.containsKey(event.getName())) {
                    throw new IllegalArgumentException("An event named '" + event.getName() + "' already exists.");
                }
            }

            for (final Event event : parsedEvents) {
                events.put(event.getName(), event);
                eventMarketMakers.put(event.getName(), uploaderName);
                uploader.addMarketMakerEvent(event.getName());
            }
        }
    }

    @Override
    public List<UserSummaryDTO> getAllUsers()
    {
        final List<UserSummaryDTO> result = new ArrayList<>();
        for (final User user : this.users.values()) {
            final List<String> relevantEventNames = new ArrayList<>();
            for (final Event event : this.events.values()) {
                final boolean isOwner = user.getName().equals(eventMarketMakers.get(event.getName()));
                final boolean participates;
                synchronized (event) {
                    participates = isParticipant(user.getName(), event);
                }
                if (isOwner || participates) {
                    relevantEventNames.add(event.getName());
                }
            }
            final boolean isMarketMaker = user.getMarketMakerForEvents() != null && !user.getMarketMakerForEvents().isEmpty();
            result.add(new UserSummaryDTO(user.getName(), user.getBalance(), user.isBlocked(), isMarketMaker, relevantEventNames, DtoMapper.balanceHistoryOf(user), DtoMapper.accountHistoryOf(user)));
        }
        return result;
    }

    @Override
    public void registerUser(final String name) throws Exception
    {
        requireCleanName(name, "Username");

        synchronized (users) {
            for (final String existingName : users.keySet()) {
                if (existingName.equalsIgnoreCase(name)) {
                    throw new IllegalArgumentException("Username '" + name + "' is already taken.");
                }
            }
            users.put(name, new User(name, 0.0, new ArrayList<>()));
        }
    }

    @Override
    public void depositCash(final String userName, final double amount) throws Exception
    {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        final User user = requireUser(userName);
        user.increaseBalance(amount, "Deposit");
    }

    @Override
    public EventDetailsDTO getEventDetails(final String eventName)
    {
        final Event event = requireEvent(eventName);
        synchronized (event) {
            return DtoMapper.mapToDetailsDTO(event, eventMarketMakers.get(event.getName()));
        }
    }

    @Override
    public ReceiptDTO buyShares(final String memberName, final String eventName, final int optionIndex, final int quantity) throws Exception
    {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        final Event event = requireEvent(eventName);

        synchronized (event) {
            requireActiveEvent(event, eventName);
            requireValidOptionIndex(event, optionIndex);

            final User buyer = requireNonBlockedUser(memberName);

            final double cost = event.calculateCost(optionIndex, quantity);
            final double commission = commissionCalculator.calculate(cost, event.getCommission(), event.getCommissionType(), CommissionType.ON_PURCHASE);
            final double totalCost = cost + commission;

            buyer.decreaseBalance(totalCost, "Bought " + quantity + " shares of \"" + event.getOptions().get(optionIndex).getName() + "\" in \"" + eventName + "\"");
            if (buyer.getBalance() < 0) {
                buyer.setBlocked(true);
            }

            event.increaseAccountBalance(cost);
            event.executePurchase(memberName, optionIndex, quantity, cost, commission);
            event.addParticipant(memberName);

            if (commission > 0) {
                event.recordCommissionPaid(memberName, commission);
                final User mm = users.get(eventMarketMakers.get(eventName));
                if (mm != null) {
                    mm.increaseBalance(commission, "Commission from " + memberName + "'s purchase in \"" + eventName + "\"");
                }
            }

            return new ReceiptDTO(cost, commission, totalCost, event.getCommissionType() == CommissionType.ON_PURCHASE, DtoMapper.mapToDetailsDTO(event, eventMarketMakers.get(eventName)));
        }
    }

    @Override
    public void openEvent(final String mmName, final String eventName) throws Exception
    {
        final Event event = requireEvent(eventName);

        final User mm = requireAssignedMarketMaker(mmName, eventName);

        synchronized (event) {
            if (event.getStatus() != EventStatus.NOT_ACTIVE) {
                throw new IllegalArgumentException("Event '" + eventName + "' cannot be opened (current status: " + event.getStatus() + ").");
            }
            if (mm.isBlocked()) {
                throw new IllegalArgumentException("Event '" + eventName + "' cannot be opened because Market Maker '" + mmName + "' has a negative balance and cannot cover the cost of subsidizing it (blocked).");
            }

            final double cost = event.calculateOpeningCost();
            mm.decreaseBalance(cost, "Opened \"" + eventName + "\" (" + event.openingCostLabel() + ")");
            if (mm.getBalance() < 0) {
                mm.setBlocked(true);
            }
            event.applyOpening(mmName, cost);
            event.activate();
        }
    }

    @Override
    public void closeEvent(final String mmName, final String eventName, final int winningOptionIndex) throws Exception
    {
        final Event event = requireEvent(eventName);

        final User mm = requireAssignedMarketMaker(mmName, eventName);

        synchronized (event) {
            if (event.getStatus() != EventStatus.ACTIVE) {
                throw new IllegalArgumentException("Event '" + eventName + "' cannot be closed (current status: " + event.getStatus() + ").");
            }

            if (winningOptionIndex < 0 || winningOptionIndex >= event.getOptions().size()) {
                throw new IllegalArgumentException("Invalid winning option selection.");
            }

            event.close(winningOptionIndex);
            eventCloser.closeEvent(event, winningOptionIndex, mm);

            final double leftover = event.getAccountBalance();
            if (leftover > 0) {
                event.decreaseAccountBalance(leftover);
                mm.increaseBalance(leftover, "Leftover funds returned from \"" + eventName + "\"");
                event.recordPayoutReceived(mm.getName(), leftover);
            }
        }
    }

    @Override
    public void submitOrder(final String userName, final String eventName, final int optionIndex, final OrderSide side, final double price, final int quantity) throws Exception
    {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        final Event event = requireEvent(eventName);
        if (!(event instanceof OrderBookEvent)) {
            throw new IllegalArgumentException("Event '" + eventName + "' is not an Order Book event.");
        }

        synchronized (event) {
            requireActiveEvent(event, eventName);
            requireValidOptionIndex(event, optionIndex);

            requireNonBlockedUser(userName);

            final OrderBookEvent obEvent = (OrderBookEvent) event;
            obEvent.addParticipant(userName);

            if (side == OrderSide.SELL) {
                final int held = obEvent.getHoldings().get(userName, optionIndex);
                final int alreadyOffered = OrderBookTradeExecutor.sellReservedQuantity(obEvent.getMarket().getBook(optionIndex), userName);
                final int available = held - alreadyOffered;
                if (available < quantity) {
                    throw new IllegalArgumentException("User '" + userName + "' only has " + available + " unreserved shares of this option available to sell (holds " + held + ", " + alreadyOffered + " already offered in other open asks), cannot sell " + quantity + ".");
                }
            }

            final TradeOutcome outcome = obEvent.getMarket().submit(userName, optionIndex, side, price, quantity);

            for (final Fill fill : outcome.getFills()) {
                orderBookTradeExecutor.applyFill(obEvent, optionIndex, fill);
            }
            for (final Mint mint : outcome.getMints()) {
                orderBookTradeExecutor.applyMint(obEvent, mint);
            }
        }
    }

    @Override
    public void createLmsrEvent(final String creatorName, final String name, final String description,
                           final int commission, final CommissionType commissionType,
                           final List<String> optionNames, final int b) throws Exception
    {
        final User creator = requireEventCreator(creatorName, name, description, commission, commissionType);
        final List<Option> options = EventFactory.buildOptions(optionNames);
        final Event created = EventFactory.buildLmsrEvent(name, description.trim(), commission, commissionType, options, b);
        registerCreatedEvent(created, creatorName, creator);
    }

    @Override
    public void createOrderBookEvent(final String creatorName, final String name, final String description,
                           final int commission, final CommissionType commissionType,
                           final List<String> optionNames, final boolean allowMint, final int initial, final int d) throws Exception
    {
        final User creator = requireEventCreator(creatorName, name, description, commission, commissionType);
        final List<Option> options = EventFactory.buildOptions(optionNames);
        final Event created = EventFactory.buildOrderBookEvent(name, description.trim(), commission, commissionType, options, allowMint, initial, d);
        registerCreatedEvent(created, creatorName, creator);
    }

    // ============================================================================
    // Helpers - private, everything below is called from the public methods above.
    // ============================================================================

    private boolean isParticipant(final String userName, final Event event) {
        return event.getParticipants().contains(userName);
    }

    private static void requireCleanName(final String name, final String label) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be empty.");
        }
        if (!name.equals(name.trim())) {
            throw new IllegalArgumentException(label + " cannot start or end with whitespace.");
        }
    }

    private void requireActiveEvent(final Event event, final String eventName) {
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot trade on Event '" + eventName + "' because it is not active (current status: " + event.getStatus() + ").");
        }
    }

    private User requireAssignedMarketMaker(final String mmName, final String eventName) {
        final String assignedMm = eventMarketMakers.get(eventName);
        if (assignedMm == null || !assignedMm.equals(mmName)) {
            throw new IllegalArgumentException("User '" + mmName + "' is not the Market Maker for Event '" + eventName + "'.");
        }
        final User mm = users.get(mmName);
        if (mm == null) {
            throw new IllegalArgumentException("Market Maker user '" + mmName + "' does not exist.");
        }
        return mm;
    }

    private Event requireEvent(final String eventName) {
        if (eventName == null) {
            throw new IllegalArgumentException("Event name cannot be null.");
        }
        final Event event = events.get(eventName);
        if (event == null) {
            throw new IllegalArgumentException("Event '" + eventName + "' does not exist.");
        }
        return event;
    }

    private User requireUser(final String userName) {
        if (userName == null) {
            throw new IllegalArgumentException("Username cannot be null.");
        }
        final User user = users.get(userName);
        if (user == null) {
            throw new IllegalArgumentException("User '" + userName + "' does not exist.");
        }
        return user;
    }

    private User requireNonBlockedUser(final String userName) {
        final User user = requireUser(userName);
        if (user.isBlocked()) {
            throw new IllegalArgumentException("User '" + userName + "' is blocked due to a negative balance and cannot perform further actions.");
        }
        return user;
    }

    private void requireValidOptionIndex(final Event event, final int optionIndex) {
        if (optionIndex < 0 || optionIndex >= event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid option selection.");
        }
    }

    private User requireEventCreator(final String creatorName, final String name, final String description,
                                      final int commission, final CommissionType commissionType) {
        final User creator = requireUser(creatorName);
        if (creator.isBlocked()) {
            throw new IllegalArgumentException("User '" + creatorName + "' has a negative balance and cannot create new events.");
        }

        requireCleanName(name, "Event name");
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Event description cannot be empty.");
        }
        if (commissionType == null) {
            throw new IllegalArgumentException("A commission type must be chosen.");
        }
        if (commission < Event.COMMISSION_VALUE_MIN || commission > Event.COMMISSION_VALUE_MAX) {
            throw new IllegalArgumentException("Commission value must be between " + Event.COMMISSION_VALUE_MIN
                + " and " + Event.COMMISSION_VALUE_MAX + " (got " + commission + ").");
        }
        return creator;
    }

    private void registerCreatedEvent(final Event created, final String creatorName, final User creator) {
        synchronized (events) {
            if (events.containsKey(created.getName())) {
                throw new IllegalArgumentException("An event named '" + created.getName() + "' already exists.");
            }
            events.put(created.getName(), created);
            eventMarketMakers.put(created.getName(), creatorName);
        }
        creator.addMarketMakerEvent(created.getName());
    }
}
