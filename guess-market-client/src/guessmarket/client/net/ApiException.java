package guessmarket.client.net;

/**
 * Thrown by ApiClient whenever the server answers with an error envelope. The message is the
 * exact text the engine's own exception carried on the server side - callers catch this the
 * same way Ex2's MainController used to catch the engine's IllegalArgumentException directly.
 */
public class ApiException extends RuntimeException
{
    public ApiException(final String message)
    {
        super(message);
    }
}
