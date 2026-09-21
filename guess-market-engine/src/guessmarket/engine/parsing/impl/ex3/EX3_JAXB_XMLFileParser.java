package guessmarket.engine.parsing.impl.ex3;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.Option;
import guessmarket.engine.models.lmsr.LmsrEvent;
import guessmarket.engine.models.orderbook.OrderBookEvent;
import guessmarket.engine.parsing.api.FileParser;
import guessmarket.engine.parsing.jaxb.generated.ex3.Commission;
import guessmarket.engine.parsing.jaxb.generated.ex3.GMEvent;
import guessmarket.engine.parsing.jaxb.generated.ex3.GMEvents;
import guessmarket.engine.parsing.jaxb.generated.ex3.GuessMarket;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses one Ex3-format XML upload into a list of events.
 * <p>
 * Unlike the Ex1/Ex2 parsers, this takes raw XML bytes directly rather than a file path - uploads
 * arrive over HTTP as content, and the server never has (or needs) a local path for them. Raw
 * bytes rather than an already-decoded String: XML declares its own encoding, and handing JAXB the
 * undecoded stream lets it read that declaration and decode correctly itself, rather than the
 * caller having already guessed a charset before JAXB ever sees the content.
 * <p>
 * Validation here covers only what a single file can know about itself: is it well-formed, and is
 * each event internally valid (name/description present, commission in range, options valid,
 * method-specific fields valid). It deliberately does NOT check a new event's name against events
 * already accumulated from other uploads - that depends on server-wide state this parser has no
 * visibility into, so it belongs at the layer that holds that state instead.
 */
public class EX3_JAXB_XMLFileParser implements FileParser {

    private static final int COMMISSION_VALUE_MAX = Event.COMMISSION_VALUE_MAX;
    private static final int COMMISSION_VALUE_MIN = Event.COMMISSION_VALUE_MIN;

    private final JAXBContext jaxbContext;

    public EX3_JAXB_XMLFileParser() {
        try {
            this.jaxbContext = JAXBContext.newInstance("guessmarket.engine.parsing.jaxb.generated.ex3");
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to initialize JAXB context for Ex3", e);
        }
    }

    @Override
    public String getFileType() {
        return "XML file (Ex3)";
    }

    @Override
    public List<Event> parse(InputStream xml) throws Exception {
        GuessMarket guessMarket;
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            guessMarket = (GuessMarket) unmarshaller.unmarshal(xml);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Error: The XML file is not well-formed (" + describeXmlSyntaxError(e) + ").", e);
        }

        return parseEvents(guessMarket.getGMEvents());
    }

    /** Pulls a human-readable reason out of a JAXBException, which almost never has its own message - the real reason sits in its linked cause. */
    private String describeXmlSyntaxError(JAXBException e) {
        Throwable cause = e.getLinkedException() != null ? e.getLinkedException() : e.getCause();
        if (cause instanceof SAXParseException) {
            SAXParseException sax = (SAXParseException) cause;
            return sax.getMessage() + " [line " + sax.getLineNumber() + ", column " + sax.getColumnNumber() + "]";
        }
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return "the file does not contain valid XML";
    }

    private List<Event> parseEvents(GMEvents gmEvents) {
        if (gmEvents == null || gmEvents.getGMEvent() == null || gmEvents.getGMEvent().isEmpty()) {
            throw new IllegalArgumentException("Error: File must contain at least one GM-event!");
        }

        List<Event> parsedEvents = new ArrayList<>();
        Set<String> namesSeenInThisFile = new HashSet<>();

        int position = 1;
        for (GMEvent gmEvent : gmEvents.getGMEvent()) {
            Event parsedEvent = parseEvent(gmEvent, position);

            if (!namesSeenInThisFile.add(parsedEvent.getName().toLowerCase())) {
                throw new IllegalArgumentException("Error in Event at position " + position + ": duplicate event name found ('" + parsedEvent.getName() + "') within this file!");
            }

            parsedEvents.add(parsedEvent);
            position++;
        }
        return parsedEvents;
    }

    private Event parseEvent(GMEvent gmEvent, int position) {
        String name = gmEvent.getName() != null ? gmEvent.getName().trim() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Error in Event at position " + position + ": event name cannot be empty!");
        }

        String description = gmEvent.getDescription() != null ? gmEvent.getDescription().trim() : "";
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Error in Event '" + name + "': event description cannot be empty!");
        }

        int commissionValue = parseCommissionValue(gmEvent.getCommission(), name);
        CommissionType commissionType = parseCommissionType(gmEvent.getCommission(), name);
        List<Option> options = parseOptions(gmEvent, name);

        if (gmEvent.getGMMethod() == null) {
            throw new IllegalArgumentException("Error in Event '" + name + "': GM-method element is missing!");
        }

        if (gmEvent.getGMMethod().getGMLMSR() != null) {
            int b = gmEvent.getGMMethod().getGMLMSR().getB();
            if (b <= 0) {
                throw new IllegalArgumentException("Error in Event '" + name + "': GM-LMSR 'b' element must be a positive integer!");
            }
            return new LmsrEvent(name, description, commissionValue, commissionType, options, b);
        }

        if (gmEvent.getGMMethod().getGMOrderBook() != null) {
            boolean allowMint = parseAllowMint(gmEvent.getGMMethod().getGMOrderBook().getAllowMint(), name);
            int initial = gmEvent.getGMMethod().getGMOrderBook().getInitial();
            int d = gmEvent.getGMMethod().getGMOrderBook().getD();

            // Not spelled out by the "Ex1-level checks" instruction - Order Book didn't exist in
            // Ex1. Applying the same bounds Ex2 already enforced rather than leaving them
            // unchecked, since an event with a negative/zero 'd' would misbehave later rather than
            // fail cleanly here. Documented as an assumption in the README.
            if (initial < 0) {
                throw new IllegalArgumentException("Error in Event '" + name + "': GM-order-book 'initial' must not be negative!");
            }
            if (d <= 0) {
                throw new IllegalArgumentException("Error in Event '" + name + "': GM-order-book 'd' (base value) must be a positive integer!");
            }

            return new OrderBookEvent(name, description, commissionValue, commissionType, options, allowMint, initial, d);
        }

        throw new IllegalArgumentException("Error in Event '" + name + "': unknown GM-method type!");
    }

    private int parseCommissionValue(Commission commission, String eventName) {
        if (commission == null) {
            throw new IllegalArgumentException("Error in Event '" + eventName + "': commission is missing!");
        }
        int parsedCommission = commission.getValue();
        if (parsedCommission < COMMISSION_VALUE_MIN || parsedCommission > COMMISSION_VALUE_MAX) {
            throw new IllegalArgumentException("Error in Event '" + eventName + "': commission value must be between " + COMMISSION_VALUE_MIN + " and " + COMMISSION_VALUE_MAX + "!");
        }
        return parsedCommission;
    }

    private CommissionType parseCommissionType(Commission commission, String eventName) {
        if (commission == null || commission.getType() == null) {
            throw new IllegalArgumentException("Error in Event '" + eventName + "': commission type is missing!");
        }
        String typeStr = commission.getType().trim();
        if (typeStr.equals("on-purchase")) {
            return CommissionType.ON_PURCHASE;
        } else if (typeStr.equals("on-close")) {
            return CommissionType.ON_CLOSE;
        } else {
            throw new IllegalArgumentException("Error in Event '" + eventName + "': invalid commission type!");
        }
    }

    private boolean parseAllowMint(String allowMintRaw, String eventName) {
        if (allowMintRaw == null) {
            throw new IllegalArgumentException("Error in Event '" + eventName + "': GM-order-book 'allow-mint' is missing!");
        }
        String value = allowMintRaw.trim();
        if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else {
            throw new IllegalArgumentException("Error in Event '" + eventName + "': GM-order-book 'allow-mint' must be 'true' or 'false' (found '" + value + "')!");
        }
    }

    private List<Option> parseOptions(GMEvent gmEvent, String eventName) {
        if (gmEvent.getGMOptions() == null || gmEvent.getGMOptions().getGMOption() == null) {
            throw new IllegalArgumentException("Error in Event '" + eventName + "': GM-options must have exactly two GM-option elements!");
        }

        List<Option> options = new ArrayList<>();
        Set<String> optionNamesSeen = new HashSet<>();

        for (String optionNameRaw : gmEvent.getGMOptions().getGMOption()) {
            String optionName = optionNameRaw.trim();
            if (optionName.isEmpty()) {
                throw new IllegalArgumentException("Error in Event '" + eventName + "': GM-option must have a name!");
            }
            if (!optionNamesSeen.add(optionName.toLowerCase())) {
                throw new IllegalArgumentException("Error in Event '" + eventName + "': duplicate option name found ('" + optionName + "')!");
            }
            options.add(new Option(optionName));
        }

        if (options.size() != 2) {
            throw new IllegalArgumentException("Error in Event '" + eventName + "': GM-options must have exactly two GM-option elements!");
        }

        return options;
    }
}
