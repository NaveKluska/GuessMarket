package guessmarket.client.net;

/**
 * Holds the name of the currently logged-in user for this running client. Set once, right after
 * login/register succeeds, and read from everywhere else (which requests to send, who "me" is
 * in the Users screen).
 */
public final class Session
{
    private static String userName;

    private Session()
    {
    }

    public static String getUserName()
    {
        return userName;
    }

    public static void setUserName(final String name)
    {
        userName = name;
    }
}
