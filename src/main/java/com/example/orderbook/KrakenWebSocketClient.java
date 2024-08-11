package com.example.orderbook;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class KrakenWebSocketClient extends WebSocketClient {
    private static final Logger logger = LoggerFactory.getLogger(KrakenWebSocketClient.class);
    private static final String KRAKEN_WS_URL = "wss://ws.kraken.com/";
    private static final Gson gson = new Gson();

    private final OrderBook orderBook = new OrderBook();

    public KrakenWebSocketClient() throws URISyntaxException {
        super(new URI(KRAKEN_WS_URL));
    }

    public void connectToWebSocket() {
        connect();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("Connected to Kraken WebSocket");
        subscribeToOrderBook("XBT/USD");
    }

    @Override
    public void onMessage(String message) {
        logger.debug("Received message: {}", message);

        try {
            if (message.trim().startsWith("[")) {
                JsonArray jsonArray = gson.fromJson(message, JsonArray.class);
                orderBook.processOrderBookArrayMessage(jsonArray);
            } else {
                JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                handleEventMessage(jsonMessage);
            }
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse message", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("Disconnected from Kraken WebSocket: {} - {}", code, reason);
    }

    @Override
    public void onError(Exception ex) {
        logger.error("WebSocket error", ex);
    }

    private void subscribeToOrderBook(String pair) {
        JsonObject subscribeMessage = new JsonObject();
        subscribeMessage.addProperty("event", "subscribe");

        JsonObject subscription = new JsonObject();
        subscription.addProperty("name", "book");
        subscribeMessage.add("subscription", subscription);

        JsonArray pairsArray = new JsonArray();
        pairsArray.add(pair);
        subscribeMessage.add("pair", pairsArray);

        send(subscribeMessage.toString());
    }

    private void handleEventMessage(JsonObject message) {
        if (message.has("event")) {
            String event = message.get("event").getAsString();
            if ("subscriptionStatus".equals(event)) {
                logger.info("Subscription status: {}", message);
            } else if ("error".equals(event)) {
                logger.error("Error: {}", message);
            }
        }
    }
}
