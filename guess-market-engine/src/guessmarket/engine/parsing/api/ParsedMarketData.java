// Was FileParser's return type when it bundled events + users. Ex3's parser only ever returns
// events (no GM-users in that schema), so FileParser.parse() returns List<Event> directly now.
// Kept for reference rather than deleted; not compiled, not called from anywhere.
/*
package guessmarket.engine.parsing.api;

import guessmarket.engine.models.Event;
import guessmarket.engine.models.User;
import java.util.List;

public class ParsedMarketData {
    private final List<Event> events;
    private final List<User> users;

    public ParsedMarketData(List<Event> events, List<User> users) {
        this.events = events;
        this.users = users;
    }

    public List<Event> getEvents() {
        return events;
    }

    public List<User> getUsers() {
        return users;
    }
}
*/
