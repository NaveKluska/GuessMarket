package guessmarket.engine.models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class OrderBookEvent extends Event {
    
    // As discussed, we are storing all info from the XML (allowMint, initial, d)
    // exactly as it is given to avoid losing data that might be needed by the engine later.
    private final boolean allowMint;
    private final int initial;
    private final int d;
    
    private final List<PriorityQueue<Order>> buyOrders;
    private final List<PriorityQueue<Order>> sellOrders;
    
    // Store the last transaction price for each option to answer "getOptionProbability"
    private final List<Double> lastTransactionPrices;

    private static class BuyOrderComparator implements Comparator<Order>, java.io.Serializable {
        @Override
        public int compare(Order a, Order b) {
            int priceCmp = Double.compare(b.getPrice(), a.getPrice()); // reversed
            return priceCmp != 0 ? priceCmp : a.getTimestamp().compareTo(b.getTimestamp());
        }
    }

    private static class SellOrderComparator implements Comparator<Order>, java.io.Serializable {
        @Override
        public int compare(Order a, Order b) {
            int priceCmp = Double.compare(a.getPrice(), b.getPrice());
            return priceCmp != 0 ? priceCmp : a.getTimestamp().compareTo(b.getTimestamp());
        }
    }

    public OrderBookEvent(int id, String name, String description, int commissionValue,
                          CommissionType commissionType, List<Option> options, 
                          boolean allowMint, int initial, int d) {
        super(id, name, description, commissionValue, commissionType, options);
        this.allowMint = allowMint;
        this.initial = initial;
        this.d = d;
        
        this.buyOrders = new ArrayList<>();
        this.sellOrders = new ArrayList<>();
        this.lastTransactionPrices = new ArrayList<>();
        
        for (int i = 0; i < options.size(); i++) {
            // Buy orders: highest price first, then earliest timestamp
            buyOrders.add(new PriorityQueue<>(new BuyOrderComparator()));
                
            // Sell orders: lowest price first, then earliest timestamp
            sellOrders.add(new PriorityQueue<>(new SellOrderComparator()));
                
            // Initial price can default to 0
            lastTransactionPrices.add(0.0);
        }
    }

    public boolean isAllowMint() {
        return allowMint;
    }

    public int getInitial() {
        return initial;
    }

    public int getD() {
        return d;
    }

    @Override
    public double getOptionProbability(int optionIndex) {
        return lastTransactionPrices.get(optionIndex);
    }
    
    public double getBid(int optionIndex) {
        PriorityQueue<Order> queue = buyOrders.get(optionIndex);
        return queue.isEmpty() ? 0.0 : queue.peek().getPrice();
    }
    
    public double getAsk(int optionIndex) {
        PriorityQueue<Order> queue = sellOrders.get(optionIndex);
        return queue.isEmpty() ? 0.0 : queue.peek().getPrice();
    }
    
    public double getMid(int optionIndex) {
        double bid = getBid(optionIndex);
        double ask = getAsk(optionIndex);
        if (bid == 0.0 && ask == 0.0) return 0.0;
        if (bid == 0.0) return ask;
        if (ask == 0.0) return bid;
        return (bid + ask) / 2.0;
    }
    
    public double getSpread(int optionIndex) {
        double bid = getBid(optionIndex);
        double ask = getAsk(optionIndex);
        if (bid == 0.0 || ask == 0.0) return 0.0;
        return ask - bid;
    }
    
    public double getLastPrice(int optionIndex) {
        return lastTransactionPrices.get(optionIndex);
    }

    @Override
    public double calculateCost(int optionIndex, int quantity) {
        throw new UnsupportedOperationException("Order Book events do not have a fixed cost. Use placeOrder instead.");
    }
    
    public static class TradeExecution {
        public final String buyer;
        public final String seller; // null for a minted trade: payment goes to the event's own account, not a counterparty
        public final String optionName;
        public final int quantity;
        public final double price;
        public final boolean isMinted; // true = a brand-new share pair was minted, no real seller

        public TradeExecution(String buyer, String seller, String optionName, int quantity, double price, boolean isMinted) {
            this.buyer = buyer;
            this.seller = seller;
            this.optionName = optionName;
            this.quantity = quantity;
            this.price = price;
            this.isMinted = isMinted;
        }
    }

    /**
     * Process an incoming order and return the trade executions it produced for the
     * engine to settle. Same-option BUY/SELL orders cross normally. If that leaves a
     * BUY order unfilled and this event allows minting, it can still be filled via
     * MINT: a resting BUY on the event's OTHER option, whose price plus this order's
     * price reaches the base value `d`, together fund a brand-new share pair
     * (Appendix B) - the resting order fills in full at its own price, and this
     * order fills at the price complementary to `d`.
     */
    public List<TradeExecution> processOrder(Order newOrder) {
        List<TradeExecution> executions = new ArrayList<>();
        int optIndex = newOrder.getOptionIndex();
        PriorityQueue<Order> buys = buyOrders.get(optIndex);
        PriorityQueue<Order> sells = sellOrders.get(optIndex);

        if (newOrder.getType() == Order.Type.BUY) {
            // Try to match against resting sell orders on the same option first.
            while (newOrder.getQuantity() > 0 && !sells.isEmpty()) {
                Order bestSell = sells.peek();
                if (newOrder.getPrice() >= bestSell.getPrice()) {
                    int tradeQty = Math.min(newOrder.getQuantity(), bestSell.getQuantity());
                    double tradePrice = bestSell.getPrice();
                    executions.add(new TradeExecution(newOrder.getUserName(), bestSell.getUserName(),
                        getOptions().get(optIndex).getName(), tradeQty, tradePrice, false));
                    newOrder.reduceQuantity(tradeQty);
                    bestSell.reduceQuantity(tradeQty);
                    if (bestSell.getQuantity() == 0) sells.poll();
                    lastTransactionPrices.set(optIndex, tradePrice);
                } else {
                    break;
                }
            }
            // If still unfilled and minting is allowed, look for a resting BUY on the
            // OTHER option whose price plus ours reaches the base value d - that pair
            // of buyers mints a new share pair between them.
            if (allowMint && newOrder.getQuantity() > 0) {
                int otherIndex = 1 - optIndex;
                PriorityQueue<Order> otherBuys = buyOrders.get(otherIndex);
                while (newOrder.getQuantity() > 0 && !otherBuys.isEmpty()
                        && newOrder.getPrice() + otherBuys.peek().getPrice() >= d) {
                    Order otherBuy = otherBuys.peek();
                    int mintQty = Math.min(newOrder.getQuantity(), otherBuy.getQuantity());
                    double otherPrice = otherBuy.getPrice();
                    double newOrderPrice = d - otherPrice; // complementary price to the base value

                    executions.add(new TradeExecution(otherBuy.getUserName(), null,
                        getOptions().get(otherIndex).getName(), mintQty, otherPrice, true));
                    executions.add(new TradeExecution(newOrder.getUserName(), null,
                        getOptions().get(optIndex).getName(), mintQty, newOrderPrice, true));

                    newOrder.reduceQuantity(mintQty);
                    otherBuy.reduceQuantity(mintQty);
                    if (otherBuy.getQuantity() == 0) otherBuys.poll();

                    lastTransactionPrices.set(optIndex, newOrderPrice);
                    lastTransactionPrices.set(otherIndex, otherPrice);
                }
            }
            if (newOrder.getQuantity() > 0) buys.add(newOrder);
        } else {
            // Try to match against resting buy orders
            while (newOrder.getQuantity() > 0 && !buys.isEmpty()) {
                Order bestBuy = buys.peek();
                if (newOrder.getPrice() <= bestBuy.getPrice()) {
                    int tradeQty = Math.min(newOrder.getQuantity(), bestBuy.getQuantity());
                    double tradePrice = bestBuy.getPrice();
                    executions.add(new TradeExecution(bestBuy.getUserName(), newOrder.getUserName(),
                        getOptions().get(optIndex).getName(), tradeQty, tradePrice, false));
                    newOrder.reduceQuantity(tradeQty);
                    bestBuy.reduceQuantity(tradeQty);
                    if (bestBuy.getQuantity() == 0) buys.poll();
                    lastTransactionPrices.set(optIndex, tradePrice);
                } else {
                    break;
                }
            }
            if (newOrder.getQuantity() > 0) sells.add(newOrder);
        }
        return executions;
    }

    public void recordTransaction(String buyerName, String optionName, int quantity, double price) {
        getTransactions().add(new Transaction(buyerName, optionName, quantity, price));
        recordPriceSnapshot();
    }

    public List<Order> getPendingOrders() {
        List<Order> pending = new ArrayList<>();
        for (PriorityQueue<Order> q : buyOrders) pending.addAll(q);
        for (PriorityQueue<Order> q : sellOrders) pending.addAll(q);
        return pending;
    }
}
