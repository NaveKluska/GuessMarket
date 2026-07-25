import java.util.ArrayList;

public class Engine
{
    private final List<Event> events;

    public Engine()
    {
        events = new ArrayList<Event>();
    }

    // 1. Load the system data (XML)
    public void loadData(String filePath)
    {
        // TODO: Implement loading logic
    }

    // 2. Show all events
    public List<EventDTO> getAllEvents()
    {
        // TODO: Implement getting all events
        return null;
    }

    // 3. Show event status
    public EventDTO getEventDetails(String eventId)
    {
        // TODO: Implement getting specific event details
        return null;
    }

    // 4. Participate in an event (Buy shares)
    public ReceiptDTO buyShares(String eventId, String choice, int quantity) throws Exception
    {
        // TODO: Implement buying logic
        return null;
    }

    // 5. Close event
    public void closeEvent(String eventId, String winningChoice) throws Exception
    {
        // TODO: Implement closing logic
    }

    // 6. Exit system (Bonus prep for saving state)
    public void shutdown() {
        // TODO: Implement shutdown/save logic
    }


}