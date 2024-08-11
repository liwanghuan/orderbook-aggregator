package com.gsr.orderbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class OrderBookTest {

    private OrderBook orderBook;
    private Logger logger;

    @BeforeEach
    public void setUp() {
        logger = mock(Logger.class);
        // Use reflection to set the logger in the OrderBook class
        OrderBook.setLogger(logger); // Add this method in OrderBook to set the logger for testing purposes
        orderBook = new OrderBook();
    }

    @Test
    public void testProcessOrderBookMessage() {
        JsonObject message = new JsonObject();
        JsonArray bids = new JsonArray();
        JsonArray asks = new JsonArray();

        JsonArray bid1 = new JsonArray();
        bid1.add(0.18475);
        bid1.add(3788.90392422);
        bids.add(bid1);

        JsonArray ask1 = new JsonArray();
        ask1.add(0.18479);
        ask1.add(16155.08885299);
        asks.add(ask1);

        message.add("bids", bids);
        message.add("asks", asks);

        orderBook.processOrderBookMessage(message);

        // Assertions to verify the bids and asks were updated correctly
        assertEquals(0.18475, orderBook.getHighestBid());
        assertEquals(0.18479, orderBook.getLowestAsk());
    }

    @Test
    public void testSanityCheck() {
        JsonObject message = new JsonObject();
        JsonArray bids = new JsonArray();
        JsonArray asks = new JsonArray();

        JsonArray bid1 = new JsonArray();
        bid1.add(0.18475);
        bid1.add(3788.90392422);
        bids.add(bid1);

        JsonArray ask1 = new JsonArray();
        ask1.add(0.18479);
        ask1.add(16155.08885299);
        asks.add(ask1);

        message.add("bids", bids);
        message.add("asks", asks);

        orderBook.processOrderBookMessage(message);

        orderBook.performSanityChecks();
        verify(logger, never()).warn(anyString()); // Ensure no warnings were logged
    }

    @Test
    public void testGenerateAndLogCandle() {
        // Simulate some data updates
        JsonObject message = new JsonObject();
        JsonArray bids = new JsonArray();
        JsonArray asks = new JsonArray();

        JsonArray bid1 = new JsonArray();
        bid1.add(0.18475);
        bid1.add(3788.90392422);
        bids.add(bid1);

        JsonArray ask1 = new JsonArray();
        ask1.add(0.18479);
        ask1.add(16155.08885299);
        asks.add(ask1);

        message.add("bids", bids);
        message.add("asks", asks);

        orderBook.processOrderBookMessage(message);
        orderBook.updateCandleData();
        orderBook.generateAndLogCandle();

        // Verify logging of candle data
        verify(logger).info(anyString(), anyLong(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }
}
