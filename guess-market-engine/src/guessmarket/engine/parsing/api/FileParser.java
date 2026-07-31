package guessmarket.engine.parsing.api;

import guessmarket.engine.models.Event;
import java.util.List;

public interface FileParser {
    
    /**
     * Returns the human-readable name of the file format this parser handles (e.g., "XML file").
     */
    String getFileType();

    /**
     * Parses the file at the given path and returns a list of events.
     */
    List<Event> parse(String filePath) throws Exception;
}
