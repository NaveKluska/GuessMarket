import java.util.ArrayList;
import java.util.List;

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
    public List<EventSummaryDTO> getAllEvents()
    {
        List<EventSummaryDTO> result = new ArrayList<EventSummaryDTO>();

        for (final Event event : events)
        {
            EventSummaryDTO eventSummary = new EventSummaryDTO(event);
            result.add(eventSummary);
        }

        return result;
    }

    // 3. Show event status
    public EventDetailsDTO getEventDetails(final int eventId)
    {
        for 

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