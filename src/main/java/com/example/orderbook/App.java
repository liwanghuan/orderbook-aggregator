package com.example.orderbook;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final String KRAKEN_WS_URL = "wss://ws.kraken.com/";
    private static final Gson gson = new Gson();

    private static TreeMap<Double, Double> bids = new TreeMap<>((a, b) -> b.compareTo(a)); // Descending order
    private static TreeMap<Double, Double> asks = new TreeMap<>(); // Ascending order

    public static void main(String[] args) {
        logger.info("Orderbook Aggregator is starting...");
        try {
            KrakenWebSocketClient client = new KrakenWebSocketClient(new URI(KRAKEN_WS_URL));
            client.connect();
        } catch (URISyntaxException e) {
            logger.error("Invalid WebSocket URI", e);
        }
    }

    private static class KrakenWebSocketClient extends WebSocketClient {

        public KrakenWebSocketClient(URI serverUri) {
            super(serverUri);
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
                    // Message is a JSON array
                    com.google.gson.JsonArray jsonArray = gson.fromJson(message, com.google.gson.JsonArray.class);
                    processOrderBookArrayMessage(jsonArray);
                } else {
                    // Message is a JSON object
                    JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);

                    if (jsonMessage.has("event")) {
                        // Handle event messages like subscription status
                        String event = jsonMessage.get("event").getAsString();
                        if ("subscriptionStatus".equals(event)) {
                            logger.info("Subscription status: {}", jsonMessage);
                        } else if ("error".equals(event)) {
                            logger.error("Error: {}", jsonMessage);
                        }
                    }
                }
            } catch (JsonSyntaxException e) {
                logger.error("Failed to parse message", e);
            }
        }

        private void processOrderBookArrayMessage(com.google.gson.JsonArray jsonArray) {
            // Here you can process the array message, assuming it's an order book update
            if (jsonArray.size() > 1 && jsonArray.get(1).isJsonObject()) {
                JsonObject orderBookUpdate = jsonArray.get(1).getAsJsonObject();
                processOrderBookMessage(orderBookUpdate);
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

            // Add the subscription details
            JsonObject subscription = new JsonObject();
            subscription.addProperty("name", "book");
            subscribeMessage.add("subscription", subscription);

            // The pair field must be an array
            com.google.gson.JsonArray pairsArray = new com.google.gson.JsonArray();
            pairsArray.add(pair);
            subscribeMessage.add("pair", pairsArray);

            // Send the subscription message
            send(subscribeMessage.toString());
        }

        private void processOrderBookMessage(JsonObject message) {
            if (message.has("bids")) {
                updateOrderBook(bids, message.getAsJsonArray("bids"));
            }
            if (message.has("asks")) {
                updateOrderBook(asks, message.getAsJsonArray("asks"));
            }
            // Additional processing can be added here (e.g., logging the state of the order book)
        }

        private void updateOrderBook(TreeMap<Double, Double> orderBookSide, com.google.gson.JsonArray updates) {
            for (com.google.gson.JsonElement update : updates) {
                com.google.gson.JsonArray updateEntry = update.getAsJsonArray();
                double price = updateEntry.get(0).getAsDouble();
                double quantity = updateEntry.get(1).getAsDouble();
                if (quantity == 0) {
                    orderBookSide.remove(price); // Remove the order if quantity is 0
                } else {
                    orderBookSide.put(price, quantity); // Update or add the order
                }
            }
            logger.debug("Updated order book side: {}", orderBookSide);
        }
    }
}
