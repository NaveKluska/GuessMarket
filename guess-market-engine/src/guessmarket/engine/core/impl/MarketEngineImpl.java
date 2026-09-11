package guessmarket.engine.core.impl;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.TransactionDTO;
import guessmarket.dto.UserSummaryDTO;
import guessmarket.dto.lmsr.LmsrEventDetailsDTO;
import guessmarket.dto.orderbook.OptionBookDTO;
import guessmarket.dto.orderbook.OrderBookEventDetailsDTO;
import guessmarket.dto.orderbook.OrderDTO;
import guessmarket.dto.orderbook.ParticipantHoldingDTO;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.EventStatus;
import guessmarket.engine.models.lmsr.LmsrEvent;
import guessmarket.engine.models.orderbook.Fill;
import guessmarket.engine.models.orderbook.MarketQuote;
import guessmarket.engine.models.orderbook.Mint;
import guessmarket.engine.models.orderbook.Order;
import guessmarket.engine.models.orderbook.OrderBookEvent;
import guessmarket.engine.models.orderbook.OrderSide;
import guessmarket.engine.models.orderbook.TradeOutcome;
import guessmarket.engine.models.Option;
import guessmarket.engine.models.Transaction;
import guessmarket.engine.models.User;
import guessmarket.engine.billing.api.CommissionCalculator;
import guessmarket.engine.parsing.api.FileParser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarketEngineImpl implements MarketEngine
{
    public static final double COST_OF_SHARE = 1.0;
    
    private final Map<Integer, Event> events;
    private final Map<String, User> users;
    private final Map<Integer, String> eventMarketMakers;
    private final FileParser parser;
    private final CommissionCalculator commissionCalculator;
    private boolean isDataLoaded;

    public MarketEngineImpl(final FileParser parser, final CommissionCalculator commissionCalculator)
    {
        this.parser = parser;
        this.commissionCalculator = commissionCalculator;
        this.events = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
        this.eventMarketMakers = new ConcurrentHashMap<>();
        this.isDataLoaded = false;
    }

    @Override
    public void loadData(String filePath) throws Exception
    {
        if (!isPathOnlyEnglishCharactersAndStandardSymbols(filePath)) {
            throw new IllegalArgumentException("Error: Only English characters and standard path symbols are allowed in the file path.");
        }
        final guessmarket.engine.parsing.api.ParsedMarketData parsedData = parser.parse(filePath);
        final List<Event> loadedEvents = parsedData.getEvents();
        this.events.clear();
        for (final Event event : loadedEvents) {
            this.events.put(event.getId(), event);
        }

        final List<User> loadedUsers = parsedData.getUsers();
        this.users.clear();
        this.eventMarketMakers.clear();
        if (loadedUsers != null) {
            for (final User user : loadedUsers) {
                this.users.put(user.getName(), user);
                if (user.getMarketMakerForEvents() != null) {
                    for (final Integer mmEventId : user.getMarketMakerForEvents()) {
                        this.eventMarketMakers.put(mmEventId, user.getName());
                    }
                }
            }
        }

        this.isDataLoaded = true;
    }

    @Override
    public List<EventSummaryDTO> getAllEvents()
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final List<EventSummaryDTO> result = new ArrayList<>();

        for (final Event event : this.events.values())
        {
            result.add(mapToSummaryDTO(event));
        }

        return result;
    }

    @Override
    public List<EventSummaryDTO> getActiveEvents()
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final List<EventSummaryDTO> result = new ArrayList<>();

        for (final Event event : this.events.values())
        {
            if (event.getActiveStatus())
            {
                result.add(mapToSummaryDTO(event));
            }
        }

        return result;
    }

    @Override
    public EventDetailsDTO getEventDetails(final int eventId)
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final Event event = this.events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist.");
        }
        return mapToDetailsDTO(event);
    }

    @Override
    public List<UserSummaryDTO> getAllUsers()
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final List<UserSummaryDTO> result = new ArrayList<>();
        for (final User user : this.users.values()) {
            final List<Integer> activeEventIds = new ArrayList<>();
            for (final Event event : this.events.values()) {
                if (event.getStatus() == EventStatus.ACTIVE && isParticipant(user.getName(), event)) {
                    activeEventIds.add(event.getId());
                }
            }
            final boolean isMarketMaker = user.getMarketMakerForEvents() != null && !user.getMarketMakerForEvents().isEmpty();
            result.add(new UserSummaryDTO(user.getName(), user.getBalance(), user.isBlocked(), isMarketMaker, activeEventIds));
        }
        return result;
    }

    private boolean isParticipant(final String userName, final Event event) {
        return event.getParticipants().contains(userName);
    }

    @Override
    public ReceiptDTO buyShares(final String memberName, final int eventId, final int optionIndex, final int quantity) throws Exception
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        final Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist.");
        }

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot trade on Event " + eventId + " because it is not active (current status: " + event.getStatus() + ").");
        }

        if (optionIndex < 0 || optionIndex >= event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid option selection.");
        }

        final User buyer = users.get(memberName);
        if (buyer == null) {
            throw new IllegalArgumentException("User '" + memberName + "' does not exist.");
        }
        if (buyer.isBlocked()) {
            throw new IllegalArgumentException("User '" + memberName + "' is blocked due to a negative balance and cannot perform further actions.");
        }

        final double cost = event.calculateCost(optionIndex, quantity);
        final double commission = commissionCalculator.calculate(cost, event.getCommission(), event.getCommissionType(), CommissionType.ON_PURCHASE);
        final double totalCost = cost + commission;

        buyer.decreaseBalance(totalCost);
        if (buyer.getBalance() < 0) {
            buyer.setBlocked(true);
        }

        event.increaseAccountBalance(cost);
        event.executePurchase(memberName, optionIndex, quantity, cost, commission);
        event.addParticipant(memberName);

        if (commission > 0) {
            final User mm = users.get(eventMarketMakers.get(eventId));
            if (mm != null) {
                mm.increaseBalance(commission);
            }
        }

        return new ReceiptDTO(cost, commission, totalCost, event.getCommissionType() == CommissionType.ON_PURCHASE, mapToDetailsDTO(event));
    }

    @Override
    public void submitOrder(final String userName, final int eventId, final int optionIndex, final OrderSide side, final double price, final int quantity) throws Exception
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        final Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist.");
        }
        if (!(event instanceof OrderBookEvent)) {
            throw new IllegalArgumentException("Event " + eventId + " is not an Order Book event.");
        }
        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot trade on Event " + eventId + " because it is not active (current status: " + event.getStatus() + ").");
        }
        if (optionIndex < 0 || optionIndex >= event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid option selection.");
        }

        final User trader = users.get(userName);
        if (trader == null) {
            throw new IllegalArgumentException("User '" + userName + "' does not exist.");
        }
        if (trader.isBlocked()) {
            throw new IllegalArgumentException("User '" + userName + "' is blocked due to a negative balance and cannot perform further actions.");
        }

        final OrderBookEvent obEvent = (OrderBookEvent) event;
        obEvent.addParticipant(userName);

        if (side == OrderSide.SELL) {
            final int held = obEvent.getHoldings().get(userName, optionIndex);
            if (held < quantity) {
                throw new IllegalArgumentException("User '" + userName + "' only holds " + held + " shares of this option, cannot sell " + quantity + ".");
            }
            obEvent.getHoldings().decrease(userName, optionIndex, quantity);
        }

        final TradeOutcome outcome = obEvent.getMarket().submit(userName, optionIndex, side, price, quantity);

        for (final Fill fill : outcome.getFills()) {
            applyFill(obEvent, optionIndex, fill);
        }
        for (final Mint mint : outcome.getMints()) {
            applyMint(obEvent, mint);
        }
    }

    private void applyFill(final OrderBookEvent obEvent, final int optionIndex, final Fill fill) {
        final boolean restingIsBuy = fill.getRestingOrder().getSide() == OrderSide.BUY;
        final String buyerName = restingIsBuy ? fill.getRestingOrder().getUserName() : fill.getIncomingOrder().getUserName();
        final String sellerName = restingIsBuy ? fill.getIncomingOrder().getUserName() : fill.getRestingOrder().getUserName();

        final double tradeValue = fill.getPrice() * fill.getQuantity();
        final double commission = commissionCalculator.calculate(tradeValue, obEvent.getCommission(), obEvent.getCommissionType(), CommissionType.ON_PURCHASE);

        final User buyer = users.get(buyerName);
        if (buyer != null) {
            buyer.decreaseBalance(tradeValue + commission);
            if (buyer.getBalance() < 0) {
                buyer.setBlocked(true);
            }
            obEvent.getHoldings().increase(buyerName, optionIndex, fill.getQuantity());
        }

        final User seller = users.get(sellerName);
        if (seller != null) {
            seller.increaseBalance(tradeValue);
        }

        creditCommissionToMm(obEvent, commission);
    }

    private void applyMint(final OrderBookEvent obEvent, final Mint mint) {
        applyMintSide(obEvent, mint.getOrderA().getUserName(), mint.getOptionIndexA(), mint.getPriceA(), mint.getQuantity());
        applyMintSide(obEvent, mint.getOrderB().getUserName(), mint.getOptionIndexB(), mint.getPriceB(), mint.getQuantity());
    }

    private void applyMintSide(final OrderBookEvent obEvent, final String userName, final int optionIndex, final double price, final int quantity) {
        final double cost = price * quantity;
        final double commission = commissionCalculator.calculate(cost, obEvent.getCommission(), obEvent.getCommissionType(), CommissionType.ON_PURCHASE);

        final User user = users.get(userName);
        if (user != null) {
            user.decreaseBalance(cost + commission);
            if (user.getBalance() < 0) {
                user.setBlocked(true);
            }
            obEvent.getHoldings().increase(userName, optionIndex, quantity);
        }

        obEvent.increaseAccountBalance(cost);
        creditCommissionToMm(obEvent, commission);
    }

    private void creditCommissionToMm(final OrderBookEvent obEvent, final double commission) {
        if (commission > 0) {
            final User mm = users.get(eventMarketMakers.get(obEvent.getId()));
            if (mm != null) {
                mm.increaseBalance(commission);
            }
        }
    }

    @Override
    public void openEvent(final String mmName, final int eventId) throws Exception
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist.");
        }

        final User mm = requireAssignedMarketMaker(mmName, eventId);

        if (event.getStatus() != EventStatus.NOT_ACTIVE) {
            throw new IllegalArgumentException("Event " + eventId + " cannot be opened (current status: " + event.getStatus() + ").");
        }
        if (mm.isBlocked()) {
            throw new IllegalArgumentException("User '" + mmName + "' is blocked due to a negative balance and cannot perform further actions.");
        }

        if (event instanceof LmsrEvent) {
            final double subsidy = ((LmsrEvent) event).calculateInitialSubsidy();
            if (mm.getBalance() < subsidy) {
                throw new IllegalArgumentException("User '" + mmName + "' has insufficient funds (" + mm.getBalance() + ") to open Event " + eventId + " (requires a subsidy of " + subsidy + ").");
            }
            mm.decreaseBalance(subsidy);
            event.increaseAccountBalance(subsidy);
            event.addParticipant(mmName);
            event.activate();
        } else if (event instanceof OrderBookEvent) {
            final OrderBookEvent obEvent = (OrderBookEvent) event;
            final double cost = obEvent.getInitial() * (double) obEvent.getD();
            if (mm.getBalance() < cost) {
                throw new IllegalArgumentException("User '" + mmName + "' has insufficient funds (" + mm.getBalance() + ") to open Event " + eventId + " (requires " + cost + ").");
            }
            mm.decreaseBalance(cost);
            event.increaseAccountBalance(cost);
            for (int i = 0; i < event.getOptions().size(); i++) {
                obEvent.getHoldings().increase(mmName, i, obEvent.getInitial());
            }
            obEvent.addParticipant(mmName);
            event.activate();
        } else {
            throw new IllegalStateException("Unknown event type: " + event.getClass().getSimpleName());
        }
    }

    @Override
    public void closeEvent(final String mmName, final int eventId, final int winningOptionIndex) throws Exception
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist.");
        }

        final User mm = requireAssignedMarketMaker(mmName, eventId);

        if (event.getStatus() != EventStatus.ACTIVE) {
            throw new IllegalArgumentException("Event " + eventId + " cannot be closed (current status: " + event.getStatus() + ").");
        }
        if (mm.isBlocked()) {
            throw new IllegalArgumentException("User '" + mmName + "' is blocked due to a negative balance and cannot perform further actions.");
        }

        if (winningOptionIndex < 0 || winningOptionIndex >= event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid winning option selection.");
        }

        event.close(winningOptionIndex);

        if (event instanceof LmsrEvent) {
            closeLmsrEvent(event, winningOptionIndex, mm);
        } else if (event instanceof OrderBookEvent) {
            closeOrderBookEvent((OrderBookEvent) event, winningOptionIndex, mm);
        }

        final double leftover = event.getAccountBalance();
        if (leftover > 0.01) {
            event.decreaseAccountBalance(leftover);
            mm.increaseBalance(leftover);
        }
    }

    private void closeLmsrEvent(final Event event, final int winningOptionIndex, final User mm) {
        final Option winningOption = event.getOptions().get(winningOptionIndex);
        for (final Transaction transaction : event.getTransactions()) {
            if (transaction.getOptionName().equals(winningOption.getName())) {
                payOutWinner(event, transaction.getUserName(), transaction.getQuantity() * COST_OF_SHARE, mm);
            }
        }
    }

    private void closeOrderBookEvent(final OrderBookEvent obEvent, final int winningOptionIndex, final User mm) {
        for (final Map.Entry<String, Integer> holder : obEvent.getHoldings().holdersOf(winningOptionIndex).entrySet()) {
            payOutWinner(obEvent, holder.getKey(), holder.getValue() * (double) obEvent.getD(), mm);
        }
    }

    private void payOutWinner(final Event event, final String winnerName, final double winAmount, final User mm) {
        final double closeCommission = commissionCalculator.calculate(winAmount, event.getCommission(), event.getCommissionType(), CommissionType.ON_CLOSE);
        final double payout = winAmount - closeCommission;

        event.decreaseAccountBalance(winAmount);
        if (closeCommission > 0) {
            event.collectCommission(closeCommission);
            mm.increaseBalance(closeCommission);
        }

        final User winner = users.get(winnerName);
        if (winner != null) {
            winner.increaseBalance(payout);
        }
    }

    private User requireAssignedMarketMaker(final String mmName, final int eventId) {
        final String assignedMm = eventMarketMakers.get(eventId);
        if (assignedMm == null || !assignedMm.equals(mmName)) {
            throw new IllegalArgumentException("User '" + mmName + "' is not the Market Maker for Event " + eventId + ".");
        }
        final User mm = users.get(mmName);
        if (mm == null) {
            throw new IllegalArgumentException("Market Maker user '" + mmName + "' does not exist.");
        }
        return mm;
    }

    @Override
    public void saveState(final String filePath) throws Exception
    {
        if (!isPathOnlyEnglishCharactersAndStandardSymbols(filePath)) {
            throw new IllegalArgumentException("Error: Only English characters and standard path symbols are allowed in the file path.");
        }
        if (!isDataLoaded) {
            throw new IllegalStateException("No data is currently loaded to save.");
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath + ".dat"))) {
            oos.writeObject(new ConcurrentHashMap<>(events));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadState(final String filePath) throws Exception
    {
        if (!isPathOnlyEnglishCharactersAndStandardSymbols(filePath)) {
            throw new IllegalArgumentException("Error: Only English characters and standard path symbols are allowed in the file path.");
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath + ".dat"))) {
            Map<Integer, Event> loaded = (Map<Integer, Event>) ois.readObject();
            events.clear();
            events.putAll(loaded);
            this.isDataLoaded = true;
        }
    }

    @Override
    public void shutdown() throws Exception
    {
        // TODO: Implement shutdown/save logic
    }

    // ---- DTO Mapping Helpers ----

    private EventSummaryDTO mapToSummaryDTO(final Event event) {
        final List<String> optionNames = new ArrayList<>();
        for (final Option option : event.getOptions()) {
            optionNames.add(option.getName());
        }

        return new EventSummaryDTO(
            event.getId(),
            event.getName(),
            event.getDescription(),
            event.getCommission(),
            event.getCommissionType().name(),
            optionNames,
            event.getStatus().name(),
            (event instanceof LmsrEvent) ? "LMSR" : "ORDER_BOOK"
        );
    }

    private EventDetailsDTO mapToDetailsDTO(final Event event) {
        if (event instanceof LmsrEvent) {
            return mapLmsrDetailsDTO(event);
        }
        return mapOrderBookDetailsDTO((OrderBookEvent) event);
    }

    private LmsrEventDetailsDTO mapLmsrDetailsDTO(final Event event) {
        final List<OptionDTO> optionDTOs = new ArrayList<>();
        final List<Option> options = event.getOptions();
        for (int i = 0; i < options.size(); i++) {
            final Option option = options.get(i);
            optionDTOs.add(new OptionDTO(option.getName(), option.getSharesBought(), event.getOptionProbability(i)));
        }

        final List<TransactionDTO> transactionDTOs = new ArrayList<>();
        for (final Transaction tx : event.getTransactions()) {
            transactionDTOs.add(new TransactionDTO(tx.getUserName(), tx.getOptionName(), tx.getQuantity(), tx.getPricePaid(), tx.getTimestamp()));
        }

        return new LmsrEventDetailsDTO(
            event.getId(), event.getName(), event.getDescription(), event.getCommission(), event.getCommissionType().name(),
            event.getStatus().name(), event.getAccountBalance(), event.getTotalCommissionCollected(),
            optionDTOs, transactionDTOs, event.getWinningOptionName()
        );
    }

    private OrderBookEventDetailsDTO mapOrderBookDetailsDTO(final OrderBookEvent event) {
        final List<OptionBookDTO> optionBooks = new ArrayList<>();
        final List<Option> options = event.getOptions();
        for (int i = 0; i < options.size(); i++) {
            final Option option = options.get(i);
            final MarketQuote quote = event.getQuote(i);
            final List<OrderDTO> bids = toOrderDTOs(event.getMarket().getBook(i).bestBuyOrdersFirst());
            final List<OrderDTO> asks = toOrderDTOs(event.getMarket().getBook(i).bestSellOrdersFirst());
            optionBooks.add(new OptionBookDTO(option.getName(), quote.getLast(), quote.getBid(), quote.getAsk(), quote.getMid(), quote.getSpread(), bids, asks));
        }

        final List<ParticipantHoldingDTO> participants = new ArrayList<>();
        for (final String participantName : event.getParticipants()) {
            final List<Integer> holdings = new ArrayList<>();
            double estimatedValue = 0.0;
            for (int i = 0; i < options.size(); i++) {
                final int quantity = event.getHoldings().get(participantName, i);
                holdings.add(quantity);
                final MarketQuote quote = event.getQuote(i);
                final Double priceEstimate = quote.getMid() != null ? quote.getMid() : quote.getLast();
                estimatedValue += quantity * (priceEstimate != null ? priceEstimate : event.getD() / 2.0);
            }
            participants.add(new ParticipantHoldingDTO(participantName, holdings, estimatedValue));
        }

        return new OrderBookEventDetailsDTO(
            event.getId(), event.getName(), event.getDescription(), event.getCommission(), event.getCommissionType().name(),
            event.getStatus().name(), event.getAccountBalance(), event.getD(), event.isAllowMint(),
            optionBooks, participants, event.getWinningOptionName()
        );
    }

    private List<OrderDTO> toOrderDTOs(final List<Order> orders) {
        final List<OrderDTO> result = new ArrayList<>();
        for (final Order order : orders) {
            result.add(new OrderDTO(order.getUserName(), order.getQuantity(), order.getPrice()));
        }
        return result;
    }

    private boolean isPathOnlyEnglishCharactersAndStandardSymbols(String path) {
        for (char c : path.toCharArray()) {
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || 
                  c == '\\' || c == '/' || c == '.' || c == ':' || c == '_' || c == '-' || c == ' ')) {
                return false;
            }
        }
        return true;
    }
}
