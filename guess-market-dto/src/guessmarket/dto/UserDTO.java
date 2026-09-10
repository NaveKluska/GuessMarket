package guessmarket.dto;

import java.io.Serializable;
import java.util.List;

public class UserDTO implements Serializable {
    private final String name;
    private final double balance;
    private final List<Integer> marketMakerForEvents;
    private final List<PortfolioItemDTO> portfolio;
    private final List<Double> balanceHistory;

    public UserDTO(String name, double balance, List<Integer> marketMakerForEvents, List<PortfolioItemDTO> portfolio, List<Double> balanceHistory) {
        this.name = name;
        this.balance = balance;
        this.marketMakerForEvents = marketMakerForEvents;
        this.portfolio = portfolio;
        this.balanceHistory = balanceHistory;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public List<Integer> getMarketMakerForEvents() {
        return marketMakerForEvents;
    }

    public List<PortfolioItemDTO> getPortfolio() {
        return portfolio;
    }

    public List<Double> getBalanceHistory() {
        return balanceHistory;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "name='" + name + '\'' +
                ", balance=" + balance +
                ", marketMakerForEvents=" + marketMakerForEvents +
                ", portfolio=" + portfolio +
                '}';
    }
}
