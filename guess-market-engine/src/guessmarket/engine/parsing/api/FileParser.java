package guessmarket.engine.parsing.api;

import guessmarket.engine.models.Event;
import java.io.InputStream;
import java.util.List;

public interface FileParser {

    /**
     * Returns the human-readable name of the file format this parser handles (e.g., "XML file").
     */
    String getFileType();

    /**
     * Parses XML content and returns the events it describes.
     * <p>
     * Takes raw bytes, not already-decoded text - the XML itself declares its own encoding, and
     * letting the parser read that declaration directly (rather than the caller guessing a charset
     * up front) is the only way encoding is handled correctly in every case.
     */
    List<Event> parse(InputStream xml) throws Exception;
}
