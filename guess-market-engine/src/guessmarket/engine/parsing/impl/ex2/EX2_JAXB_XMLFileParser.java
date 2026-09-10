package guessmarket.engine.parsing.impl.ex2;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.lmsr.LmsrEvent;
import guessmarket.engine.models.Option;
import guessmarket.engine.models.orderbook.OrderBookEvent;
import guessmarket.engine.models.User;
import guessmarket.engine.parsing.api.FileParser;
import guessmarket.engine.parsing.api.ParsedMarketData;
import guessmarket.engine.parsing.jaxb.generated.ex2.Commission;
import guessmarket.engine.parsing.jaxb.generated.ex2.GMEvent;
import guessmarket.engine.parsing.jaxb.generated.ex2.GMEvents;
import guessmarket.engine.parsing.jaxb.generated.ex2.GMUser;
import guessmarket.engine.parsing.jaxb.generated.ex2.GMUsers;
import guessmarket.engine.parsing.jaxb.generated.ex2.GuessMarket;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EX2_JAXB_XMLFileParser implements FileParser {

    private static final int COMMISSION_VALUE_MAX = 90;
    private static final int COMMISSION_VALUE_MIN = 0;

    private final JAXBContext jaxbContext;

    public EX2_JAXB_XMLFileParser() {
        try {
            this.jaxbContext = JAXBContext.newInstance("guessmarket.engine.parsing.jaxb.generated.ex2");
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to initialize JAXB context for Ex2", e);
        }
    }

    @Override
    public String getFileType() {
        return "XML file (Ex2)";
    }

    @Override
    public ParsedMarketData parse(String filePath) throws Exception {
        if (!filePath.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("Error: File at " + filePath + " must end with .xml");
        }

        File xmlFile = new File(filePath);
        if (!xmlFile.exists()) {
            throw new IllegalArgumentException("Error: File at " + filePath + " does not exist!");
        }

        GuessMarket guessMarket;
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            guessMarket = (GuessMarket) unmarshaller.unmarshal(xmlFile);
        } catch (JAXBException e) {
            throw new Exception("Error parsing XML file with JAXB: " + e.getMessage(), e);
        }

        List<User> users = parseUsers(guessMarket.getGMUsers());
        List<Event> events = parseEvents(guessMarket.getGMEvents());

        validateMarketMakers(users, events);

        return new ParsedMarketData(events, users);
    }

    private List<User> parseUsers(GMUsers gmUsers) {
        List<User> parsedUsers = new ArrayList<>();
        Set<String> parsedNames = new HashSet<>();

        if (gmUsers == null || gmUsers.getGMUser() == null) {
            return parsedUsers;
        }

        for (GMUser gmUser : gmUsers.getGMUser()) {
            String name = gmUser.getName() != null ? gmUser.getName().trim() : "";
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Error: User name cannot be empty!");
            }
            if (!parsedNames.add(name.toLowerCase())) {
                throw new IllegalArgumentException("Error: Duplicate User name found ('" + name + "')!");
            }

            int initialCash = gmUser.getInitialCash();
            if (initialCash <= 0) {
                throw new IllegalArgumentException("Error for User '" + name + "': Initial cash must be strictly greater than 0 (found " + initialCash + ")!");
            }

            List<Integer> marketMakerEvents = new ArrayList<>();
            if (gmUser.getGMMarketMaker() != null && gmUser.getGMMarketMaker().getEvent() != null) {
                for (guessmarket.engine.parsing.jaxb.generated.ex2.Event mmEvent : gmUser.getGMMarketMaker().getEvent()) {
                    marketMakerEvents.add(mmEvent.getId());
                }
            }

            parsedUsers.add(new User(name, initialCash, marketMakerEvents));
        }

        return parsedUsers;
    }

    private List<Event> parseEvents(GMEvents gmEvents) {
        List<Event> parsedEvents = new ArrayList<>();
        Set<Integer> parsedIds = new HashSet<>();

        if (gmEvents == null || gmEvents.getGMEvent() == null || gmEvents.getGMEvent().isEmpty()) {
            throw new IllegalArgumentException("Error: File must contain at least one GM-event!");
        }

        int index = 1;
        for (GMEvent gmEvent : gmEvents.getGMEvent()) {
            Event parsedEvent = parseEvent(gmEvent);

            if (!parsedIds.add(parsedEvent.getId())) {
                throw new IllegalArgumentException("Error in Event at position " + index + ": Duplicate Event ID found (" + parsedEvent.getId() + ")!");
            }

            parsedEvents.add(parsedEvent);
            index++;
        }
        return parsedEvents;
    }

    private Event parseEvent(GMEvent gmEvent) {
        int id = gmEvent.getId();

        String name = gmEvent.getName() != null ? gmEvent.getName().trim() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Error in Event " + id + ": Event name cannot be empty!");
        }

        String description = gmEvent.getDescription() != null ? gmEvent.getDescription().trim() : "";
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Error in Event " + id + ": Event description cannot be empty!");
        }

        int commissionValue = parseCommissionValue(gmEvent.getCommission(), id);
        CommissionType commissionType = parseCommissionType(gmEvent.getCommission(), id);

        List<Option> options = parseOptions(gmEvent, id);

        if (gmEvent.getGMMethod() == null) {
            throw new IllegalArgumentException("Error in Event " + id + ": GM-method element is missing!");
        }

        if (gmEvent.getGMMethod().getGMLMSR() != null) {
            int b = gmEvent.getGMMethod().getGMLMSR().getB();
            if (b <= 0) {
                throw new IllegalArgumentException("Error in Event " + id + ": GM-LMSR 'b' element must be a positive integer!");
            }
            return new LmsrEvent(id, name, description, commissionValue, commissionType, options, b);
        } else if (gmEvent.getGMMethod().getGMOrderBook() != null) {
            boolean allowMint = "true".equalsIgnoreCase(gmEvent.getGMMethod().getGMOrderBook().getAllowMint());
            int initial = gmEvent.getGMMethod().getGMOrderBook().getInitial();
            int d = gmEvent.getGMMethod().getGMOrderBook().getD();

            if (initial < 0) {
                throw new IllegalArgumentException("Error in Event " + id + ": GM-order-book 'initial' must not be negative!");
            }
            if (d <= 0) {
                throw new IllegalArgumentException("Error in Event " + id + ": GM-order-book 'd' (base value) must be a positive integer!");
            }

            return new OrderBookEvent(id, name, description, commissionValue, commissionType, options, allowMint, initial, d);
        } else {
            throw new IllegalArgumentException("Error in Event " + id + ": Unknown GM-method type!");
        }
    }

    private int parseCommissionValue(Commission commission, int eventId) {
        if (commission == null) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission is missing!");
        }
        int parsedCommission = commission.getValue();
        if (parsedCommission < COMMISSION_VALUE_MIN || parsedCommission > COMMISSION_VALUE_MAX) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission value must be between " + COMMISSION_VALUE_MIN + " and " + COMMISSION_VALUE_MAX + "!");
        }
        return parsedCommission;
    }

    private CommissionType parseCommissionType(Commission commission, int eventId) {
        if (commission == null || commission.getType() == null) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission type is missing!");
        }
        String typeStr = commission.getType().trim();
        if (typeStr.equals("on-purchase")) {
            return CommissionType.ON_PURCHASE;
        } else if (typeStr.equals("on-close")) {
            return CommissionType.ON_CLOSE;
        } else {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Invalid commission type!");
        }
    }

    private List<Option> parseOptions(GMEvent gmEvent, int eventId) {
        if (gmEvent.getGMOptions() == null || gmEvent.getGMOptions().getGMOption() == null) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-options must have exactly two GM-option elements!");
        }

        List<Option> options = new ArrayList<>();
        Set<String> optionNames = new HashSet<>();

        for (String optionNameRaw : gmEvent.getGMOptions().getGMOption()) {
            String optionName = optionNameRaw.trim();
            if (optionName.isEmpty()) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": GM-option must have a name!");
            }
            if (!optionNames.add(optionName.toLowerCase())) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": Duplicate option name found ('" + optionName + "')!");
            }
            options.add(new Option(optionName));
        }

        if (options.size() != 2) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-options must have exactly two GM-option elements!");
        }

        return options;
    }

    private void validateMarketMakers(List<User> users, List<Event> events) {
        Set<Integer> allEventIds = new HashSet<>();
        for (Event event : events) {
            allEventIds.add(event.getId());
        }

        Map<Integer, String> assignedByUser = new HashMap<>();
        for (User user : users) {
            for (Integer eventId : user.getMarketMakerForEvents()) {
                if (!allEventIds.contains(eventId)) {
                    throw new IllegalArgumentException("Error: User '" + user.getName() + "' is assigned as Market Maker for non-existent Event ID " + eventId);
                }
                String existingOwner = assignedByUser.putIfAbsent(eventId, user.getName());
                if (existingOwner != null) {
                    if (existingOwner.equals(user.getName())) {
                        throw new IllegalArgumentException("Error: User '" + user.getName() + "' lists Event ID " + eventId + " as Market Maker more than once!");
                    }
                    throw new IllegalArgumentException("Error: Event ID " + eventId + " has multiple Market Makers assigned ('" + existingOwner + "' and '" + user.getName() + "')!");
                }
            }
        }
        Set<Integer> assignedEventIds = assignedByUser.keySet();

        for (Integer eventId : allEventIds) {
            if (!assignedEventIds.contains(eventId)) {
                throw new IllegalArgumentException("Error: Event ID " + eventId + " does not have a designated Market Maker!");
            }
        }
    }
}
