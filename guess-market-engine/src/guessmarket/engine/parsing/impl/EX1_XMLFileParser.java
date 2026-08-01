package guessmarket.engine.parsing.impl;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.Option;
import guessmarket.engine.parsing.api.FileParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// To be clear: There are places where i limit my self to a specific number of options like only 2 options and only LMSR
// So it might be best to change the name of the implementation to EX1_XMLFileParser.java
// Will defintly not work well for EX2!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!


public class EX1_XMLFileParser implements FileParser {

    private static final int COMMISSION_VALUE_MAX = 90;
    private static final int COMMISSION_VALUE_MIN = 0;

    @Override
    public String getFileType() {
        return "XML file";
    }

    @Override
    public List<Event> parse(String filePath) throws Exception {
        Document document = getDocument(filePath);

        List<Event> parsedEvents = getParsedEvents(document);
        
        return parsedEvents;
    }

    private Document getDocument(String filePath) throws Exception {
        
        if (!filePath.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("Error: File at " + filePath + " must end with .xml");
        }

        File xmlFile = new File(filePath);
        if (!xmlFile.exists()) {
            throw new IllegalArgumentException("Error: File at " + filePath + " does not exist!");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        document.getDocumentElement().normalize();
        
        return document;
    }
        
    private List<Event> getParsedEvents(Document document) {
        List<Event> parsedEvents = new ArrayList<>();
        
        NodeList eventNodes = document.getElementsByTagName("GM-event");

        for (int i = 0; i < eventNodes.getLength(); i++) {
            Node node = eventNodes.item(i);
            
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eventElement = (Element) node;
                parsedEvents.add(parseEvent(eventElement));
            }
        }
        return parsedEvents;
    }

    private Event parseEvent(Element eventElement) {
        int id = parseId(eventElement);
        String name = parseName(eventElement, id);
        String description = parseDescription(eventElement, id);
        int commissionValue = parseCommissionValue(eventElement, id);
        CommissionType commissionType = parseCommissionType(eventElement, id);
        List<Option> options = parseOptions(eventElement, id);
        int b = parseLMSRbValue(eventElement, id);

        return new Event(id, name, description, commissionValue, commissionType, options, b);
    }

    private String parseName(Element eventElement, int eventId) {
        if (!eventElement.hasAttribute("name")) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have a name!");
        }
        return eventElement.getAttribute("name").trim();
    }
    
    private int parseId(Element eventElement) {
        if (!eventElement.hasAttribute("id")) {
            throw new IllegalArgumentException("Error: Event must have an id!");
        }
        try {
            return Integer.parseInt(eventElement.getAttribute("id").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error: Event id must be a valid integer!");
        }
    }

    private String parseDescription(Element eventElement, int eventId) {
        NodeList descriptionNodes = eventElement.getElementsByTagName("description");
        if (descriptionNodes.getLength() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one description!");
        }
        String desc = descriptionNodes.item(0).getTextContent().trim();
        if (desc.isEmpty()) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event description cannot be empty!");
        }
        return desc;
    }

    private int parseCommissionValue(Element eventElement, int eventId) {
        NodeList commissionNodes = eventElement.getElementsByTagName("comision");
        if (commissionNodes.getLength() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one commission!");
        }
        Element commissionElement = (Element) commissionNodes.item(0);
        
        String textContent = commissionElement.getTextContent().trim();
        if (textContent.isEmpty()) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission must have a value!");
        }
        try {
            int parsedCommission = Integer.parseInt(textContent);
            if (parsedCommission < COMMISSION_VALUE_MIN || parsedCommission > COMMISSION_VALUE_MAX) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": Commission value must be between " + COMMISSION_VALUE_MIN + " and " + COMMISSION_VALUE_MAX + "!");
            }
            return parsedCommission;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission value must be a valid integer!");
        }
    }

    private CommissionType parseCommissionType(Element eventElement, int eventId) {
        NodeList commissionNodes = eventElement.getElementsByTagName("comision");
        if (commissionNodes.getLength() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one commission!");
        }
        Element commissionElement = (Element) commissionNodes.item(0);
        
        // Reverted to attribute check as per standard XML schema
        if (!commissionElement.hasAttribute("type")) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission must have a type attribute!");
        }

        String commissionTypeStr = commissionElement.getAttribute("type").trim();
        if (commissionTypeStr.equalsIgnoreCase("on-purchase")) {
            return CommissionType.ON_PURCHASE;
        } else if (commissionTypeStr.equalsIgnoreCase("on-close")) {
            return CommissionType.ON_CLOSE;
        } else {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Invalid commission type!");
        }
    }

    private int parseLMSRbValue(Element eventElement, int eventId) {
        NodeList methodNodes = eventElement.getElementsByTagName("GM-method");
        if (methodNodes.getLength() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one GM-method element!");
        }
        Element methodElement = (Element) methodNodes.item(0);
        
        NodeList lmsrNodes = methodElement.getElementsByTagName("GM-LMSR");
        if (lmsrNodes.getLength() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-method must contain exactly one GM-LMSR element!");
        }
        Element lmsrElement = (Element) lmsrNodes.item(0);
        
        NodeList bNodes = lmsrElement.getElementsByTagName("b");
        if (bNodes.getLength() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-LMSR must have exactly one 'b' element!");
        }
        Element bElement = (Element) bNodes.item(0);
        
        try {
            int parsedB = Integer.parseInt(bElement.getTextContent().trim());
            if (parsedB <= 0) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": GM-LMSR 'b' element must be a positive integer!");
            }
            return parsedB;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-LMSR 'b' element must be a valid integer!");
        }
    }

    private List<Option> parseOptions(Element eventElement, int eventId) {
        NodeList optionsNodes = eventElement.getElementsByTagName("GM-options");
        if (optionsNodes.getLength() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one GM-options element!");
        }
        Element optionsElement = (Element) optionsNodes.item(0);
        NodeList optionNodes = optionsElement.getElementsByTagName("GM-option");

        if (optionNodes.getLength() != 2) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-options must have exactly two GM-option elements!");
        }
        
        List<Option> options = new ArrayList<>();
        for (int i = 0; i < optionNodes.getLength(); i++) {
            Node optionNode = optionNodes.item(i);

            if (optionNode.getNodeType() != Node.ELEMENT_NODE) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": GM-option must be an element!");
            }
            String optionName = optionNode.getTextContent().trim();
            if (optionName.isEmpty()) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": GM-option must have a name!");
            }
            options.add(new Option(optionName));
        }
        
        return options;
    }
}
