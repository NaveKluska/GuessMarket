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
import guessmarket.engine.pricing.api.PricingModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarketEngineImpl implements MarketEngine
{
    private final Map<Integer, Event> events;
    private final FileParser parser;
    private final PricingModel pricingModel;
    private final CommissionCalculator commissionCalculator;
    private boolean isDataLoaded;

    public MarketEngineImpl(final FileParser parser, final PricingModel pricingModel, final CommissionCalculator commissionCalculator)
    {
        this.parser = parser;
        this.pricingModel = pricingModel;
        this.commissionCalculator = commissionCalculator;
        this.events = new ConcurrentHashMap<>();
        this.isDataLoaded = false;
    }

    @Override
    public void loadData(String filePath) throws Exception
    {
        final List<Event> loadedEvents = parser.parse(filePath);
        events.clear();
        for (final Event event : loadedEvents) {
            events.put(event.getId(), event);
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

        for (final Event event : events.values())
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

        for (final Event event : events.values())
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

        final Event event = events.get(eventId);
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
                throw new IllegalArgumentException("Invalid option index: " + optionIndex);
            }

            final double cost = pricingModel.calculateCost(event, optionIndex, quantity);
            final double commission = commissionCalculator.calculate(event, cost);

            event.executePurchase(memberName, optionIndex, quantity, cost, commission);

            return new ReceiptDTO(cost, commission, cost + commission, new EventDetailsDTO(event));
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
                throw new IllegalArgumentException("Invalid winning option index: " + winningOptionIndex);
            }

            // 1. Close the event so no one can buy shares anymore
            event.deactivateEvent();
            final Option winningOption = event.getOptions().get(winningOptionIndex);

            // 2. If ON_CLOSE, calculate the final commissions
            if (event.getCommissionType() == CommissionType.ON_CLOSE) {
                
                // Look through every purchase ever made for this event
                for (final Transaction transaction : event.getTransactions()) {
                    
                    // If they bought the winning option, they get a payout!
                    if (transaction.getOptionName().equals(winningOption.getName())) {
                        
                        // 1 share = 1 coin payout
                        final double winAmount = transaction.getQuantity() * 1.0; 
                        final double profit = winAmount - transaction.getPricePaid();
                        
                        if (profit > 0) {
                            final double commission = profit * ((double) event.getCommission() / 100.0);
                            // Add the commission to the Engine's pool
                            event.collectCommission(commission); 
                        }
                    }
                }
            }
        }
    }

    @Override
    public void shutdown() throws Exception
    {
        // TODO: Implement shutdown/save logic
    }
}