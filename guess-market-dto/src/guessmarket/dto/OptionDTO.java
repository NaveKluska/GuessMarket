package guessmarket.dto;

public class OptionDTO
{
    private final String name;
    private final int sharesBought;
    private final double currentProbability;
    
    private double bid = 0.0;
    private double ask = 0.0;
    private double mid = 0.0;
    private double spread = 0.0;
    private double lastPrice = 0.0;

    public OptionDTO(final String name, final int sharesBought, final double currentProbability)
    {
        this.name = name;
        this.sharesBought = sharesBought;
        this.currentProbability = currentProbability;
    }

    public OptionDTO(String name, int sharesBought, double currentProbability, 
                     double bid, double ask, double mid, double spread, double lastPrice) {
        this(name, sharesBought, currentProbability);
        this.bid = bid;
        this.ask = ask;
        this.mid = mid;
        this.spread = spread;
        this.lastPrice = lastPrice;
    }

    public String getName()
    {
        return name;
    }

    public int getSharesBought()
    {
        return sharesBought;
    }

    public double getCurrentProbability()
    {
        return currentProbability;
    }
    
    public double getBid() { return bid; }
    public double getAsk() { return ask; }
    public double getMid() { return mid; }
    public double getSpread() { return spread; }
    public double getLastPrice() { return lastPrice; }
}
