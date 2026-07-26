public class Option
{
    private final String name;
    private int sharesBought;

    public Option(String name)
    {
        this.name = name;
        this.sharesBought = 0;
    }

    public String getName()
    {
        return name;
    }

    public int getSharesBought()
    {
        return sharesBought;
    }

    public void addShares(int amount)
    {
        this.sharesBought += amount;
    }
}
