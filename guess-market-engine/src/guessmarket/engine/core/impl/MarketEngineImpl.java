package guessmarket.engine.core.impl;

import guessmarket.engine.core.api.MarketEngine;
import guessmarket.engine.dto.EventDetailsDTO;
import guessmarket.engine.dto.EventSummaryDTO;
import guessmarket.engine.dto.ReceiptDTO;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarketEngineImpl implements MarketEngine
{
    public static final double COST_OF_SHARE = 1.0;
    
    private final Map<Integer, Event> events;
    private final FileParser parser;
    private final CommissionCalculator commissionCalculator;
    private boolean isDataLoaded;

    public MarketEngineImpl(final FileParser parser, final CommissionCalculator commissionCalculator)
    {
        this.parser = parser;
        this.commissionCalculator = commissionCalculator;
        this.events = new ConcurrentHashMap<>();
        this.isDataLoaded = false;
    }

    @Override
    public void loadData(String filePath) throws Exception
    {
        if (!isPathOnlyEnglishCharactersAndStandardSymbols(filePath)) {
            throw new IllegalArgumentException("Error: Only English characters and standard path symbols are allowed in the file path.");
        }
        final List<Event> loadedEvents = parser.parse(filePath);
        this.events.clear();
        for (final Event event : loadedEvents) {
            this.events.put(event.getId(), event);
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
            final EventSummaryDTO eventSummary = new EventSummaryDTO(event);
            result.add(eventSummary);
        }

        return Collections.unmodifiableList(result);
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
                final EventSummaryDTO eventSummary = new EventSummaryDTO(event);
                result.add(eventSummary);
            }
        }

        return Collections.unmodifiableList(result);
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
        return new EventDetailsDTO(event);
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

        synchronized (event) {
            if (!event.getActiveStatus()) {
                throw new IllegalArgumentException("Cannot buy shares for a closed event.");
            }

            if (optionIndex < 0 || optionIndex >= event.getOptions().size()) {
                throw new IllegalArgumentException("Invalid option selection.");
            }

            final double cost = event.calculateCost(optionIndex, quantity);
            final double commission = commissionCalculator.calculate(cost, event.getCommission(), event.getCommissionType(), CommissionType.ON_PURCHASE);

            event.executePurchase(memberName, optionIndex, quantity, cost, commission);

            return new ReceiptDTO(cost, commission, cost + commission, event.getCommissionType() == CommissionType.ON_PURCHASE, new EventDetailsDTO(event));
        }
    }

    @Override
    public void closeEvent(final int eventId, final int winningOptionIndex) throws Exception
    {
        if (!isDataLoaded) {
            throw new IllegalStateException("No " + parser.getFileType() + " is currently loaded in the system.");
        }

        final Event event = events.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("Event with ID " + eventId + " does not exist.");
        }

        synchronized (event) {
            if (!event.getActiveStatus()) {
                throw new IllegalArgumentException("Event is already closed.");
            }

            if (winningOptionIndex < 0 || winningOptionIndex >= event.getOptions().size()) {
                throw new IllegalArgumentException("Invalid winning option selection.");
            }

            final Option winningOption = event.getOptions().get(winningOptionIndex);
            event.deactivateEvent(winningOptionIndex);

            for (final Transaction transaction : event.getTransactions()) {
                if (transaction.getOptionName().equals(winningOption.getName())) {
                    final double winAmount = transaction.getQuantity() * COST_OF_SHARE; 
                    final double commission = commissionCalculator.calculate(winAmount, event.getCommission(), event.getCommissionType(), CommissionType.ON_CLOSE);
                    
                    event.collectCommission(commission);
                    event.deductFromBalance(winAmount - commission);
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