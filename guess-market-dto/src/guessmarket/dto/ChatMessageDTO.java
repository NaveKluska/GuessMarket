package guessmarket.dto;

import java.time.LocalDateTime;

public class ChatMessageDTO
{
    private final String userName;
    private final String message;
    private final LocalDateTime at;

    public ChatMessageDTO(final String userName, final String message, final LocalDateTime at)
    {
        this.userName = userName;
        this.message = message;
        this.at = at;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getMessage()
    {
        return message;
    }

    public LocalDateTime getAt()
    {
        return at;
    }
}
