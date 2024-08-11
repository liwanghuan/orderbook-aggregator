package com.gsr.orderbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;
import java.util.Timer;
import java.util.TimerTask;

public class OrderBook {
    private static Logger logger = LoggerFactory.getLogger(OrderBook.class);

    private final TreeMap<Double, Double> bids = new TreeMap<>((a, b) -> b.compareTo(a)); // Descending order
    private final TreeMap<Double, Double> asks = new TreeMap<>(); // Ascending order

    private final Timer timer = new Timer(true);
    private double openPrice = 0;
    private double highPrice = 0;
    private double lowPrice = Double.MAX_VALUE;
    private double closePrice = 0;
    private int ticks = 0;
    private long lastCandleTime = System.currentTimeMillis() / 60000 * 60000; // Start of the current minute

    public OrderBook() {
        // Schedule the task to run every minute
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                generateAndLogCandle();
                resetMinuteData();
            }
        }, 60000 - (System.currentTimeMillis() % 60000), 60000); // Delay to start at the next minute
    }

    public void processOrderBookArrayMessage(JsonArray jsonArray) {
        if (jsonArray.size() > 1 && jsonArray.get(1).isJsonObject()) {
            JsonObject orderBookUpdate = jsonArray.get(1).getAsJsonObject();
            processOrderBookMessage(orderBookUpdate);
        }
    }

    public void processOrderBookMessage(JsonObject message) {
        if (message.has("bids")) {
            updateOrderBook(bids, message.getAsJsonArray("bids"));
        }
        if (message.has("asks")) {
            updateOrderBook(asks, message.getAsJsonArray("asks"));
        }
        performSanityChecks();
        updateCandleData();
    }

    private void updateOrderBook(TreeMap<Double, Double> orderBookSide, JsonArray updates) {
        for (var update : updates) {
            JsonArray updateEntry = update.getAsJsonArray();
            double price = updateEntry.get(0).getAsDouble();
            double quantity = updateEntry.get(1).getAsDouble();
            if (quantity == 0) {
                orderBookSide.remove(price);
            } else {
                orderBookSide.put(price, quantity);
            }
        }
        logger.debug("Updated order book side: {}", orderBookSide);
    }

    public void performSanityChecks() {
        if (!bids.isEmpty() && !asks.isEmpty()) {
            double highestBid = bids.firstKey();
            double lowestAsk = asks.firstKey();

            if (highestBid >= lowestAsk) {
                logger.warn("Sanity check failed: highest bid ({}) >= lowest ask ({})", highestBid, lowestAsk);
            }
        } else {
            logger.warn("Sanity check failed: Order book is missing bids or asks.");
        }
    }

    public void updateCandleData() {
        if (!bids.isEmpty() && !asks.isEmpty()) {
            double highestBid = bids.firstKey();
            double lowestAsk = asks.firstKey();
            double midPrice = (highestBid + lowestAsk) / 2.0;

            if (ticks == 0) {
                openPrice = midPrice;
            }
            closePrice = midPrice;
            highPrice = Math.max(highPrice, midPrice);
            lowPrice = Math.min(lowPrice, midPrice);

            ticks++;
        }
    }

    public void generateAndLogCandle() {
        long currentTime = System.currentTimeMillis();
        long timestamp = lastCandleTime / 1000; // Convert to seconds

        logger.info("Candle Data - Timestamp: {}, Open: {}, High: {}, Low: {}, Close: {}, Ticks: {}",
                timestamp, openPrice, highPrice, lowPrice, closePrice, ticks);
    }

    private void resetMinuteData() {
        lastCandleTime = System.currentTimeMillis() / 60000 * 60000; // Update to start of new minute
        openPrice = 0;
        highPrice = 0;
        lowPrice = Double.MAX_VALUE;
        closePrice = 0;
        ticks = 0;
    }

    // Add getter methods for testing purposes
    public double getHighestBid() {
        return bids.isEmpty() ? 0 : bids.firstKey();
    }

    public double getLowestAsk() {
        return asks.isEmpty() ? 0 : asks.firstKey();
    }

    // Static method to set logger for testing
    public static void setLogger(Logger logger) {
       OrderBook.logger = logger;
    }
}
