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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// To be clear: There are places where i limit my self to a specific number of options like only 2 options and only LMSR
// Will definitely not work well for EX2!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

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
        Element root = document.getDocumentElement();
        if (!root.getNodeName().equals("Guess-Market")) {
            throw new IllegalArgumentException("Error: Root element must be 'Guess-Market'!");
        }
        
        List<Element> eventsNodes = getChildElementsByTagName(root, "GM-events");
        if (eventsNodes.size() != 1) {
            throw new IllegalArgumentException("Error: XML must contain exactly one 'GM-events' element!");
        }

        List<Event> parsedEvents = new ArrayList<>();
        Set<Integer> parsedIds = new HashSet<>();
        
        List<Element> eventNodes = getChildElementsByTagName(eventsNodes.get(0), "GM-event");
        if (eventNodes.isEmpty()) {
            throw new IllegalArgumentException("Error: File must contain at least one GM-event!");
        }

        for (int i = 0; i < eventNodes.size(); i++) {
            Element eventElement = eventNodes.get(i);
            Event parsedEvent = parseEvent(eventElement, i + 1);
            
            if (!parsedIds.add(parsedEvent.getId())) {
                throw new IllegalArgumentException("Error in Event at position " + (i + 1) + ": Duplicate Event ID found (" + parsedEvent.getId() + ")!");
            }
            
            parsedEvents.add(parsedEvent);
        }
        return parsedEvents;
    }

    private Event parseEvent(Element eventElement, int eventIndex) {
        int id = parseId(eventElement, eventIndex);
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
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have a name attribute!");
        }
        String name = eventElement.getAttribute("name").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event name cannot be empty!");
        }
        return name;
    }
    
    private int parseId(Element eventElement, int eventIndex) {
        List<Element> idNodes = getChildElementsByTagName(eventElement, "id");
        if (idNodes.size() != 1) {
            throw new IllegalArgumentException("Error in Event at position " + eventIndex + ": Event must have exactly one id element!");
        }
        String id = idNodes.get(0).getTextContent().trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Error in Event at position " + eventIndex + ": Event id cannot be empty!");
        }
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in Event at position " + eventIndex + ": Event id must be a valid integer!");
        }
    }

    private String parseDescription(Element eventElement, int eventId) {
        List<Element> descriptionNodes = getChildElementsByTagName(eventElement, "description");
        if (descriptionNodes.size() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one description!");
        }
        String desc = descriptionNodes.get(0).getTextContent().trim();
        if (desc.isEmpty()) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event description cannot be empty!");
        }
        return desc;
    }

    private int parseCommissionValue(Element eventElement, int eventId) {
        List<Element> commissionNodes = getChildElementsByTagName(eventElement, "comision");
        if (commissionNodes.size() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one commission!");
        }
        Element commissionElement = commissionNodes.get(0);
        
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
        List<Element> commissionNodes = getChildElementsByTagName(eventElement, "comision");
        if (commissionNodes.size() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one commission!");
        }
        Element commissionElement = commissionNodes.get(0);
        
        if (!commissionElement.hasAttribute("type")) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Commission must have a type attribute!");
        }
        String typeStr = commissionElement.getAttribute("type").trim();
        if (typeStr.equals("on-purchase")) {
            return CommissionType.ON_PURCHASE;
        } else if (typeStr.equals("on-close")) {
            return CommissionType.ON_CLOSE;
        } else {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Invalid commission type!");
        }
    }

    private int parseLMSRbValue(Element eventElement, int eventId) {
        List<Element> methodNodes = getChildElementsByTagName(eventElement, "GM-method");
        if (methodNodes.size() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one GM-method element!");
        }
        Element methodElement = methodNodes.get(0);
        
        List<Element> lmsrNodes = getChildElementsByTagName(methodElement, "GM-LMSR");
        if (lmsrNodes.size() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-method must contain exactly one GM-LMSR element!");
        }
        Element lmsrElement = lmsrNodes.get(0);
        
        List<Element> bNodes = getChildElementsByTagName(lmsrElement, "b");
        if (bNodes.size() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-LMSR must have exactly one 'b' element!");
        }
        Element bElement = bNodes.get(0);
        
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
        List<Element> optionsNodes = getChildElementsByTagName(eventElement, "GM-options");
        if (optionsNodes.size() != 1) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Event must have exactly one GM-options element!");
        }
        Element optionsElement = optionsNodes.get(0);
        
        List<Element> optionNodes = getChildElementsByTagName(optionsElement, "GM-option");
        if (optionNodes.size() != 2) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-options must have exactly two GM-option elements!");
        }
        
        List<Option> options = new ArrayList<>();
        for (int i = 0; i < optionNodes.size(); i++) {
            Element optionNode = optionNodes.get(i);
            
            String optionName = optionNode.getTextContent().trim();
            if (optionName.isEmpty()) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": GM-option must have a name!");
            }
            options.add(new Option(optionName));
        }
        
        return options;
    }

    private List<Element> getChildElementsByTagName(Element parent, String tagName) {
        List<Element> elements = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
                elements.add((Element) child);
            }
        }
        return elements;
    }
}
