package guessmarket.engine.parsing.impl;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.LmsrEvent;
import guessmarket.engine.models.Option;
import guessmarket.engine.parsing.api.FileParser;
import guessmarket.engine.parsing.jaxb.generated.Comision;
import guessmarket.engine.parsing.jaxb.generated.GMEvent;
import guessmarket.engine.parsing.jaxb.generated.GuessMarket;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EX1_JAXB_XMLFileParser implements FileParser {

    private static final int COMMISSION_VALUE_MAX = 90;
    private static final int COMMISSION_VALUE_MIN = 0;

    private final JAXBContext jaxbContext;

    public EX1_JAXB_XMLFileParser() {
        try {
            this.jaxbContext = JAXBContext.newInstance("guessmarket.engine.parsing.jaxb.generated");
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to initialize JAXB context", e);
        }
    }

    @Override
    public String getFileType() {
        return "XML file";
    }

    @Override
    public List<Event> parse(String filePath) throws Exception {
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

        return getParsedEvents(guessMarket);
    }

    private List<Event> getParsedEvents(GuessMarket guessMarket) {
        List<Event> parsedEvents = new ArrayList<>();
        Set<Integer> parsedIds = new HashSet<>();

        if (guessMarket.getGMEvents() == null || guessMarket.getGMEvents().getGMEvent() == null) {
            // return an empty list
            return parsedEvents;
        }

        int index = 1;
        for (GMEvent gmEvent : guessMarket.getGMEvents().getGMEvent()) {
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

        String name = gmEvent.getName() != null ? String.join(" ", gmEvent.getName()) : "";
        String description = gmEvent.getDescription() != null ? gmEvent.getDescription().trim() : "";
        
        int commissionValue = parseCommissionValue(gmEvent.getComision(), id);
        CommissionType commissionType = parseCommissionType(gmEvent.getComision(), id);
        
        List<Option> options = parseOptions(gmEvent, id);
        int b = parseLMSRbValue(gmEvent, id);

        return new LmsrEvent(id, name, description, commissionValue, commissionType, options, b);
    }

    private int parseCommissionValue(Comision comision, int eventId) {
        if (comision == null) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission is missing!");
        }
        int parsedCommission = comision.getValue();
        if (parsedCommission < COMMISSION_VALUE_MIN || parsedCommission > COMMISSION_VALUE_MAX) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission value must be between " + COMMISSION_VALUE_MIN + " and " + COMMISSION_VALUE_MAX + "!");
        }
        return parsedCommission;
    }

    private CommissionType parseCommissionType(Comision comision, int eventId) {
        if (comision == null || comision.getType() == null) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission type is missing!");
        }
        String typeStr = comision.getType().trim();
        if (typeStr.equals("on-purchase")) {
            return CommissionType.ON_PURCHASE;
        } else if (typeStr.equals("on-close")) {
            return CommissionType.ON_CLOSE;
        } else {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Invalid commission type!");
        }
    }

    private int parseLMSRbValue(GMEvent gmEvent, int eventId) {
        if (gmEvent.getGMMethod() == null || gmEvent.getGMMethod().getGMLMSR() == null) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-LMSR element is missing!");
        }
        int parsedB = gmEvent.getGMMethod().getGMLMSR().getB();
        if (parsedB <= 0) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-LMSR 'b' element must be a positive integer!");
        }
        return parsedB;
    }

    private List<Option> parseOptions(GMEvent gmEvent, int eventId) {
        if (gmEvent.getGMOptions() == null || gmEvent.getGMOptions().getGMOption() == null) {
            return new ArrayList<>();
        }

        List<Option> options = new ArrayList<>();
        Set<String> optionNames = new HashSet<>();

        for (String optionNameRaw : gmEvent.getGMOptions().getGMOption()) {
            String optionName = optionNameRaw.trim();
            if (!optionNames.add(optionName.toLowerCase())) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": Duplicate option name found ('" + optionName + "')!");
            }
            options.add(new Option(optionName));
        }

        return options;
    }
}
