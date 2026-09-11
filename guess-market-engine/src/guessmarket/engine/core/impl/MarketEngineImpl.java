package guessmarket.engine.core.impl;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.dto.EventDetailsDTO;
import guessmarket.dto.EventSummaryDTO;
import guessmarket.dto.OptionDTO;
import guessmarket.dto.ReceiptDTO;
import guessmarket.dto.TransactionDTO;
import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.EventStatus;
import guessmarket.engine.models.lmsr.LmsrEvent;
import guessmarket.engine.models.orderbook.OrderBookEvent;
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

        if (commission > 0) {
            final User mm = users.get(eventMarketMakers.get(eventId));
            if (mm != null) {
                mm.increaseBalance(commission);
            }
        }

        return new ReceiptDTO(cost, commission, totalCost, event.getCommissionType() == CommissionType.ON_PURCHASE, mapToDetailsDTO(event));
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
            event.activate();
        } else if (event instanceof OrderBookEvent) {
            throw new UnsupportedOperationException("Order Book events are not yet supported.");
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

        final Option winningOption = event.getOptions().get(winningOptionIndex);
        event.close(winningOptionIndex);

        for (final Transaction transaction : event.getTransactions()) {
            if (transaction.getOptionName().equals(winningOption.getName())) {
                final double winAmount = transaction.getQuantity() * COST_OF_SHARE;
                final double closeCommission = commissionCalculator.calculate(winAmount, event.getCommission(), event.getCommissionType(), CommissionType.ON_CLOSE);
                final double payout = winAmount - closeCommission;

                // Deduct the FULL winAmount here, not just the net payout - the commission slice
                // is leaving the account too, just headed to the MM instead of the winner. If we only
                // deducted "payout", the commission portion would stay counted in the account's balance
                // and then get swept into the leftover-subsidy refund below a second time.
                event.decreaseAccountBalance(winAmount);
                if (closeCommission > 0) {
                    event.collectCommission(closeCommission);
                    mm.increaseBalance(closeCommission);
                }

                final User winner = users.get(transaction.getUserName());
                if (winner != null) {
                    winner.increaseBalance(payout);
                }
            }
        }

        if (event instanceof LmsrEvent) {
            final double leftoverSubsidy = event.getAccountBalance();
            if (leftoverSubsidy > 0) {
                event.decreaseAccountBalance(leftoverSubsidy);
                mm.increaseBalance(leftoverSubsidy);
            }
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
            event.getActiveStatus()
        );
    }

    private EventDetailsDTO mapToDetailsDTO(final Event event) {
        final List<OptionDTO> optionDTOs = new ArrayList<>();
        final List<Option> options = event.getOptions();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                Option option = options.get(i);
                double prob = event.getOptionProbability(i);
                optionDTOs.add(new OptionDTO(option.getName(), option.getSharesBought(), prob));
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
            event.getAccountBalance(),
            event.getTotalCommissionCollected(),
            optionDTOs,
            transactionDTOs,
            event.getWinningOptionName()
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
