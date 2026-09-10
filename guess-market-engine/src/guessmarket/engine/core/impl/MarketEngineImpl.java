package guessmarket.engine.core.impl;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.TransactionDTO;
import guessmarket.dto.UserDTO;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.Option;
import guessmarket.engine.models.Transaction;
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
import guessmarket.engine.models.User;

public class MarketEngineImpl implements MarketEngine
{
    public static final double COST_OF_SHARE = 1.0;
    
    private final Map<Integer, Event> events;
    private final Map<String, User> users;
    private final FileParser parser;
    private final CommissionCalculator commissionCalculator;
    private boolean isDataLoaded;
    private int nextEventId = 10000; // IDs for user-created events start high to avoid collisions

    public MarketEngineImpl(final FileParser parser, final CommissionCalculator commissionCalculator)
    {
        this.parser = parser;
        this.commissionCalculator = commissionCalculator;
        this.events = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
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
        final List<User> loadedUsers = parsedData.getUsers();
        
        this.events.clear();
        for (final Event event : loadedEvents) {
            this.events.put(event.getId(), event);
        }
        
        this.users.clear();
        if (loadedUsers != null) {
            for (final User user : loadedUsers) {
                this.users.put(user.getName(), user);
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

    // I WANT HERE TO RETURN THE SAME SEMANTICS AS getAllEvents
    @Override
    public List<UserDTO> getAllUsers() {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final List<UserDTO> result = new ArrayList<>();

        for (final guessmarket.engine.models.User user : this.users.values()) {
            List<guessmarket.dto.PortfolioItemDTO> portfolioDTOs = new ArrayList<>();
            for (guessmarket.engine.models.PortfolioItem item : user.getPortfolio()) {
                portfolioDTOs.add(new guessmarket.dto.PortfolioItemDTO(item.getEventId(), item.getOptionName(), item.getQuantity()));
            }
            result.add(new UserDTO(user.getName(), user.getBalance(), user.getMarketMakerForEvents(), portfolioDTOs, new ArrayList<>(user.getBalanceHistory())));
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
    public ReceiptDTO buyShares(String memberName, int eventId, int optionIndex, int quantity) {
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

        if (!event.isStarted()) {
            throw new IllegalArgumentException("Cannot buy shares for an event that has not started.");
        }

        if (!event.getActiveStatus()) {
            throw new IllegalArgumentException("Cannot buy shares for a closed event.");
        }

        if (optionIndex < 0 || optionIndex >= event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid option selection.");
        }

        final guessmarket.engine.models.User user = this.users.get(memberName);
        if (user == null) {
            throw new IllegalArgumentException("User " + memberName + " does not exist.");
        }
        if (user.isBlocked()) {
            throw new IllegalStateException("User " + memberName + " is blocked due to a negative balance and cannot perform actions.");
        }

        final double cost = event.calculateCost(optionIndex, quantity);
        final double commission = commissionCalculator.calculate(cost, event.getCommission(), event.getCommissionType(), CommissionType.ON_PURCHASE);
        final double totalCost = cost + commission;

        if (user.getBalance() < totalCost) {
            throw new IllegalArgumentException(String.format("Insufficient funds. Cost is %.2f but balance is %.2f", totalCost, user.getBalance()));
        }

        user.deductBalance(totalCost);
        user.addShares(eventId, event.getOptions().get(optionIndex).getName(), quantity);

        event.executePurchase(memberName, optionIndex, quantity, cost, commission);

        return new ReceiptDTO(cost, commission, totalCost, event.getCommissionType() == CommissionType.ON_PURCHASE, mapToDetailsDTO(event));
    }
    
    @Override
    public void placeOrder(String userName, int eventId, int optionIndex, int quantity, double price, guessmarket.engine.models.Order.Type type) throws Exception {
        if (!isDataLoaded) throw new IllegalStateException("No data loaded.");
        if (quantity <= 0 || price <= 0) throw new IllegalArgumentException("Quantity and price must be positive.");
        
        Event event = events.get(eventId);
        if (event == null) throw new IllegalArgumentException("Event does not exist.");
        if (!event.isStarted()) throw new IllegalArgumentException("Cannot trade on an event that has not started.");
        if (!event.getActiveStatus()) throw new IllegalArgumentException("Cannot trade on a closed event.");
        if (!(event instanceof guessmarket.engine.models.OrderBookEvent)) throw new IllegalArgumentException("Not an Order Book event.");
        
        guessmarket.engine.models.OrderBookEvent obEvent = (guessmarket.engine.models.OrderBookEvent) event;

        if (price > obEvent.getD() - 0.01) {
            throw new IllegalArgumentException("Price cannot exceed d - 0.01 (" + (obEvent.getD() - 0.01) + ")");
        }

        guessmarket.engine.models.User user = users.get(userName);
        if (user == null) throw new IllegalArgumentException("User does not exist.");
        if (user.isBlocked()) throw new IllegalStateException("User " + userName + " is blocked due to a negative balance and cannot perform actions.");
        
        // Find the market maker for this event (needed for minting)
        String marketMakerName = null;
        for (guessmarket.engine.models.User u : users.values()) {
            if (u.getMarketMakerForEvents().contains(eventId)) {
                marketMakerName = u.getName();
                break;
            }
        }
        
        if (type == guessmarket.engine.models.Order.Type.BUY) {
            // Lock funds immediately
            double totalCost = price * quantity;
            if (user.getBalance() < totalCost) throw new IllegalArgumentException("Insufficient funds.");
            user.deductBalance(totalCost);
        } else {
            // Lock shares immediately
            String optionName = event.getOptions().get(optionIndex).getName();
            int owned = user.getPortfolioQuantity(eventId, optionName);
            if (owned < quantity) throw new IllegalArgumentException("Insufficient shares.");
            user.addShares(eventId, optionName, -quantity);
        }
        
        guessmarket.engine.models.Order order = new guessmarket.engine.models.Order(userName, optionIndex, type, price, quantity);
        List<guessmarket.engine.models.OrderBookEvent.TradeExecution> trades = obEvent.processOrder(order, marketMakerName);
        
        for (guessmarket.engine.models.OrderBookEvent.TradeExecution trade : trades) {
            guessmarket.engine.models.User buyerUser = users.get(trade.buyer);
            
            if (trade.isMinted) {
                // Market maker minted shares: buyer already paid (funds locked above).
                // Buyer receives shares. Market maker receives the payment.
                buyerUser.addShares(eventId, trade.optionName, trade.quantity);
                if (marketMakerName != null) {
                    guessmarket.engine.models.User mm = users.get(marketMakerName);
                    if (mm != null) mm.addFunds(trade.price * trade.quantity);
                }
            } else {
                guessmarket.engine.models.User sellerUser = users.get(trade.seller);
                if (type == guessmarket.engine.models.Order.Type.BUY) {
                    // Refund any excess we locked vs. the actual trade price
                    double refund = (price - trade.price) * trade.quantity;
                    buyerUser.addFunds(refund);
                    buyerUser.addShares(eventId, trade.optionName, trade.quantity);
                    sellerUser.addFunds(trade.price * trade.quantity);
                } else {
                    // Resting BUY filled our SELL
                    sellerUser.addFunds(trade.price * trade.quantity);
                    buyerUser.addShares(eventId, trade.optionName, trade.quantity);
                }
            }
            
            // Record transaction in event
            obEvent.recordTransaction(trade.buyer, trade.optionName, trade.quantity, trade.price);
        }
    }

    @Override
    public int createLmsrEvent(String name, String description, int commission, guessmarket.engine.models.CommissionType commissionType, java.util.List<String> optionNames, int b) {
        int id = nextEventId++;
        java.util.List<guessmarket.engine.models.Option> options = new ArrayList<>();
        for (String optName : optionNames) {
            options.add(new guessmarket.engine.models.Option(optName));
        }
        events.put(id, new guessmarket.engine.models.LmsrEvent(id, name, description, commission, commissionType, options, b));
        return id;
    }

    @Override
    public int createOrderBookEvent(String name, String description, int commission, guessmarket.engine.models.CommissionType commissionType, java.util.List<String> optionNames, boolean allowMint, int initial, int d) {
        int id = nextEventId++;
        java.util.List<guessmarket.engine.models.Option> options = new ArrayList<>();
        for (String optName : optionNames) {
            options.add(new guessmarket.engine.models.Option(optName));
        }
        events.put(id, new guessmarket.engine.models.OrderBookEvent(id, name, description, commission, commissionType, options, allowMint, initial, d));
        return id;
    }

    @Override
    public java.util.List<guessmarket.dto.PendingOrderDTO> getPendingOrders(int eventId) {
        Event event = events.get(eventId);
        if (event == null || !(event instanceof guessmarket.engine.models.OrderBookEvent)) {
            return new ArrayList<>();
        }
        guessmarket.engine.models.OrderBookEvent obEvent = (guessmarket.engine.models.OrderBookEvent) event;
        java.util.List<guessmarket.dto.PendingOrderDTO> result = new ArrayList<>();
        for (guessmarket.engine.models.Order order : obEvent.getPendingOrders()) {
            String optionName = event.getOptions().get(order.getOptionIndex()).getName();
            result.add(new guessmarket.dto.PendingOrderDTO(
                order.getUserName(),
                order.getType().name(),
                optionName,
                order.getPrice(),
                order.getQuantity()
            ));
        }
        return result;
    }

    @Override
    public void startEvent(int eventId, String userName) throws Exception {
        if (!isDataLoaded) throw new IllegalStateException("No data loaded.");
        Event event = events.get(eventId);
        if (event == null) throw new IllegalArgumentException("Event does not exist.");
        if (event.isStarted()) throw new IllegalArgumentException("Event has already started.");

        guessmarket.engine.models.User user = users.get(userName);
        if (user == null) throw new IllegalArgumentException("User does not exist.");
        if (!user.getMarketMakerForEvents().contains(eventId)) {
            throw new IllegalArgumentException("Only the designated Market Maker can start this event.");
        }

        if (event instanceof guessmarket.engine.models.LmsrEvent) {
            guessmarket.engine.models.LmsrEvent lmsr = (guessmarket.engine.models.LmsrEvent) event;
            double subsidy = lmsr.getB() * Math.log(event.getOptions().size());
            if (user.getBalance() < subsidy) {
                throw new IllegalArgumentException(String.format("Insufficient funds to start LMSR event. Requires %.2f for initial subsidy.", subsidy));
            }
            user.deductBalance(subsidy);
            event.injectFunds(subsidy);
        } else if (event instanceof guessmarket.engine.models.OrderBookEvent) {
            guessmarket.engine.models.OrderBookEvent obEvent = (guessmarket.engine.models.OrderBookEvent) event;
            double cost = obEvent.getInitial() * obEvent.getD();
            if (user.getBalance() < cost) {
                throw new IllegalArgumentException(String.format("Insufficient funds to start Order Book event. Requires %.2f for initial shares.", cost));
            }
            user.deductBalance(cost);
            event.injectFunds(cost);
            obEvent.seedInitialOrders(userName);
        }

        event.setStarted(true);
    }

    @Override
    public void closeEvent(final int eventId, final int winningOptionIndex, String userName) throws Exception
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist.");
        }

        if (!event.isStarted()) {
            throw new IllegalArgumentException("Cannot close an event that has not been started yet.");
        }
        if (!event.getActiveStatus()) {
            throw new IllegalArgumentException("Event is already closed.");
        }

        guessmarket.engine.models.User userToClose = users.get(userName);
        if (userToClose == null || !userToClose.getMarketMakerForEvents().contains(eventId)) {
            throw new IllegalArgumentException("Only the designated Market Maker can close this event.");
        }

        if (winningOptionIndex < 0 || winningOptionIndex >= event.getOptions().size()) {
            throw new IllegalArgumentException("Invalid winning option selection.");
        }

        final Option winningOption = event.getOptions().get(winningOptionIndex);
        event.deactivateEvent(winningOptionIndex);

        // Determine the payout per winning share.
        // LMSR: each winning share pays $1 (COST_OF_SHARE).
        // Order Book: each winning share pays `d` (the base unit defined in XML).
        final double payoutPerShare;
        if (event instanceof guessmarket.engine.models.OrderBookEvent) {
            payoutPerShare = ((guessmarket.engine.models.OrderBookEvent) event).getD();
        } else {
            payoutPerShare = COST_OF_SHARE;
        }

        for (final guessmarket.engine.models.Transaction transaction : event.getTransactions()) {
            if (transaction.getOptionName().equals(winningOption.getName())) {
                final double winAmount = transaction.getQuantity() * payoutPerShare;
                final double commission = commissionCalculator.calculate(transaction.getPricePaid(), event.getCommission(), event.getCommissionType(), guessmarket.engine.models.CommissionType.ON_CLOSE);
                    
                event.collectCommission(commission);
                event.deductFromBalance(winAmount - commission);
                
                guessmarket.engine.models.User user = this.users.get(transaction.getUserName());
                if (user != null) {
                    user.addFunds(winAmount - commission);
                }
            }
        }
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
            // Save both events and users so state is fully restorable.
            oos.writeObject(new ConcurrentHashMap<>(events));
            oos.writeObject(new ConcurrentHashMap<>(users));
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
            Map<Integer, Event> loadedEvents = (Map<Integer, Event>) ois.readObject();
            Map<String, guessmarket.engine.models.User> loadedUsers = (Map<String, guessmarket.engine.models.User>) ois.readObject();
            events.clear();
            events.putAll(loadedEvents);
            users.clear();
            users.putAll(loadedUsers);
            this.isDataLoaded = true;
        }
    }

    @Override
    public void addFunds(String userName, double amount) {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount to add must be positive.");
        }
        final guessmarket.engine.models.User user = this.users.get(userName);
        if (user == null) {
            throw new IllegalArgumentException("User " + userName + " does not exist.");
        }
        // Note: we deliberately allow adding funds even to a blocked user — that is the purpose of this action.
        user.addFunds(amount);
    }

    @Override
    public void shutdown() throws Exception
    {
        // TODO: Implement shutdown/save logic
    }

    // ---- DTO Mapping Helpers ----

    private EventSummaryDTO mapToSummaryDTO(final Event event) {
        final List<String> optionNames = new ArrayList<>();
        for (final guessmarket.engine.models.Option option : event.getOptions()) {
            optionNames.add(option.getName());
        }

        String method = (event instanceof guessmarket.engine.models.OrderBookEvent) ? "OrderBook" : "LMSR";

        return new EventSummaryDTO(
            event.getId(),
            event.getName(),
            event.getDescription(),
            event.getCommission(),
            event.getCommissionType().name(),
            optionNames,
            event.getActiveStatus(),
            event.isStarted(),
            method
        );
    }

    private EventDetailsDTO mapToDetailsDTO(final Event event) {
        final List<OptionDTO> optionDTOs = new ArrayList<>();
        final List<Option> options = event.getOptions();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                Option option = options.get(i);
                double prob = event.getOptionProbability(i);
                
                if (event instanceof guessmarket.engine.models.OrderBookEvent) {
                    guessmarket.engine.models.OrderBookEvent obEvent = (guessmarket.engine.models.OrderBookEvent) event;
                    optionDTOs.add(new OptionDTO(option.getName(), option.getSharesBought(), prob,
                        obEvent.getBid(i), obEvent.getAsk(i), obEvent.getMid(i), obEvent.getSpread(i), obEvent.getLastPrice(i)));
                } else {
                    optionDTOs.add(new OptionDTO(option.getName(), option.getSharesBought(), prob));
                }
            }
        }

        final List<TransactionDTO> transactionDTOs = new ArrayList<>();
        final List<Transaction> transactions = event.getTransactions();
        if (transactions != null) {
            for (final Transaction tx : transactions) {
                transactionDTOs.add(new TransactionDTO(
                    tx.getUserName(), tx.getOptionName(), tx.getQuantity(),
                    tx.getPricePaid(), tx.getTimestamp()
                ));
            }
        }

        return new EventDetailsDTO(
            event.getId(),
            event.getName(),
            event.getDescription(),
            event.getCommission(),
            event.getCommissionType().name(),
            event.getActiveStatus(),
            event.isStarted(),
            event.getAccountBalance(),
            event.getTotalCommissionCollected(),
            optionDTOs,
            transactionDTOs,
            event.getWinningOptionName(),
            event.getPriceHistory()
        );
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
