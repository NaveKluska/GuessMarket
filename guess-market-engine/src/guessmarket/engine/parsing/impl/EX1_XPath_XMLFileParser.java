package guessmarket.engine.parsing.impl;

import guessmarket.engine.models.CommissionType;
import guessmarket.engine.models.Event;
import guessmarket.engine.models.LmsrEvent;
import guessmarket.engine.models.Option;
import guessmarket.engine.parsing.api.FileParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// XPath-based parser for EX1.
// Assumes the XML is valid against the XSD schema (GM-EX1-schema.xsd),
// so structural validation is skipped. Only business/logical rules are checked.

public class EX1_XPath_XMLFileParser implements FileParser {

    private static final int COMMISSION_VALUE_MAX = 90;
    private static final int COMMISSION_VALUE_MIN = 0;

    private final XPath xpath;

    public EX1_XPath_XMLFileParser() {
        this.xpath = XPathFactory.newInstance().newXPath();
    }

    @Override
    public String getFileType() {
        return "XML file";
    }

    @Override
    public List<Event> parse(String filePath) throws Exception {
        Document document = getDocument(filePath);

        return getParsedEvents(document);
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

    private List<Event> getParsedEvents(Document document) throws Exception {
        List<Event> parsedEvents = new ArrayList<>();
        Set<Integer> parsedIds = new HashSet<>();

        NodeList eventNodes = (NodeList) xpath.evaluate("//GM-event", document, XPathConstants.NODESET);

        for (int i = 0; i < eventNodes.getLength(); i++) {
            Element eventElement = (Element) eventNodes.item(i);
            Event parsedEvent = parseEvent(eventElement, i + 1);

            if (!parsedIds.add(parsedEvent.getId())) {
                throw new IllegalArgumentException("Error in Event at position " + (i + 1) + ": Duplicate Event ID found (" + parsedEvent.getId() + ")!");
            }

            parsedEvents.add(parsedEvent);
        }
        return parsedEvents;
    }

    private Event parseEvent(Element eventElement, int eventIndex) throws Exception {
        String name = xpath.evaluate("@name", eventElement).trim();
        int id = parseId(eventElement, eventIndex);
        String description = xpath.evaluate("description", eventElement).trim();
        int commissionValue = parseCommissionValue(eventElement, id);
        CommissionType commissionType = parseCommissionType(eventElement, id);
        List<Option> options = parseOptions(eventElement, id);
        int b = parseLMSRbValue(eventElement, id);

        return new LmsrEvent(id, name, description, commissionValue, commissionType, options, b);
    }

    private int parseId(Element eventElement, int eventIndex) throws Exception {
        String id = xpath.evaluate("id", eventElement).trim();
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in Event at position " + eventIndex + ": Event id must be a valid integer!");
        }
    }

    private int parseCommissionValue(Element eventElement, int eventId) throws Exception {
        String textContent = xpath.evaluate("comision", eventElement).trim();
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

    private CommissionType parseCommissionType(Element eventElement, int eventId) throws Exception {
        String typeStr = xpath.evaluate("comision/@type", eventElement).trim();
        if (typeStr.equals("on-purchase")) {
            return CommissionType.ON_PURCHASE;
        } else if (typeStr.equals("on-close")) {
            return CommissionType.ON_CLOSE;
        } else {
            throw new IllegalArgumentException("Error in Event " + eventId + ": Invalid commission type!");
        }
    }

    private int parseLMSRbValue(Element eventElement, int eventId) throws Exception {
        String bText = xpath.evaluate("GM-method/GM-LMSR/b", eventElement).trim();
        try {
            int parsedB = Integer.parseInt(bText);
            if (parsedB <= 0) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": GM-LMSR 'b' element must be a positive integer!");
            }
            return parsedB;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error in Event " + eventId + ": GM-LMSR 'b' element must be a valid integer!");
        }
    }

    private List<Option> parseOptions(Element eventElement, int eventId) throws Exception {
        NodeList optionNodes = (NodeList) xpath.evaluate("GM-options/GM-option", eventElement, XPathConstants.NODESET);

        List<Option> options = new ArrayList<>();
        Set<String> optionNames = new HashSet<>();
        for (int i = 0; i < optionNodes.getLength(); i++) {
            String optionName = optionNodes.item(i).getTextContent().trim();
            if (!optionNames.add(optionName.toLowerCase())) {
                throw new IllegalArgumentException("Error in Event " + eventId + ": Duplicate option name found ('" + optionName + "')!");
            }
            options.add(new Option(optionName));
        }

        return options;
    }
}
