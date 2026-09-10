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
        public final String seller;
        public final String optionName;
        public final int quantity;
        public final double price;
        public final boolean isMinted; // true = filled by market maker minting, no real seller

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
     * Process an incoming order. If allowMint is true and a BUY order can't be fully matched,
     * the remaining quantity is filled by minting new shares (no real seller needed).
     * Returns a list of trade executions for the engine to settle.
     * marketMakerName is only used when allowMint=true (can be null otherwise).
     */
    public List<TradeExecution> processOrder(Order newOrder, String marketMakerName) {
        List<TradeExecution> executions = new ArrayList<>();
        int optIndex = newOrder.getOptionIndex();
        PriorityQueue<Order> buys = buyOrders.get(optIndex);
        PriorityQueue<Order> sells = sellOrders.get(optIndex);
        
        if (newOrder.getType() == Order.Type.BUY) {
            // Try to match against resting sell orders
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
            // If minting allowed and still unfilled, market maker fills the rest
            if (allowMint && newOrder.getQuantity() > 0 && marketMakerName != null) {
                double tradePrice = newOrder.getPrice();
                executions.add(new TradeExecution(newOrder.getUserName(), marketMakerName,
                    getOptions().get(optIndex).getName(), newOrder.getQuantity(), tradePrice, true));
                lastTransactionPrices.set(optIndex, tradePrice);
                newOrder.reduceQuantity(newOrder.getQuantity());
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

    /** Backwards-compatible overload (no minting). */
    public List<TradeExecution> processOrder(Order newOrder) {
        return processOrder(newOrder, null);
    }

    /**
     * Seeds the order book with initial liquidity using the 'initial' and 'd' parameters.
     * Called once after construction when a market maker is assigned.
     * Places 'initial' buy orders and 'initial' sell orders for every option,
     * stepping by 'd' cents from the midpoint (0.50).
     */
    public List<TradeExecution> seedInitialOrders(String marketMakerName) {
        List<TradeExecution> executions = new ArrayList<>();
        if (!allowMint || initial <= 0 || d <= 0) return executions;
        int optCount = getOptions().size();
        // Fair prior: equal probability, scaled to d (e.g. d=1 → midpoint=0.50 for 2 options)
        double midpoint = (double) d / optCount;
        // Each step moves the price by 1 cent (0.01), capped so prices stay in [0.01, d-0.01]
        double tickSize = 0.01;
        for (int i = 0; i < optCount; i++) {
            for (int step = 1; step <= initial; step++) {
                double bidPrice = Math.max(0.01, midpoint - step * tickSize);
                double askPrice = Math.min(d - 0.01, midpoint + step * tickSize);
                // Market maker places a resting sell at askPrice and a resting buy at bidPrice
                Order sellOrder = new Order(marketMakerName, i, Order.Type.SELL, askPrice, 1);
                Order buyOrder  = new Order(marketMakerName, i, Order.Type.BUY,  bidPrice, 1);
                sellOrders.get(i).add(sellOrder);
                buyOrders.get(i).add(buyOrder);
            }
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
