package guessmarket.engine.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ChatMessage(String userName, String message, LocalDateTime at) implements Serializable {
}
