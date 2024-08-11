# Order Book Aggregator

## Overview
This application connects to the Kraken exchange via WebSocket to receive tick-level order book data and aggregates it into 1-minute candle data.

## Features
- Real-time order book management
- 1-minute candle data calculation
- Error handling and reconnect strategies for WebSocket

## Requirements
- Java 17 (JDK 17)
- Maven (for building the project)

## Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/your-repo/orderbook-aggregator.git
cd orderbook-aggregator
```
### 2. Build the Project
```bash
mvn clean install
```
### 3. Run the Application
```bash
java -jar target/orderbook-aggregator-1.0-SNAPSHOT.jar
```
### 4. Assumptions
- The application assumes that malformed messages from the WebSocket will be ignored.
- Reconnection to WebSocket is attempted every 5 seconds if the connection is lost.
### 5. Running Tests
```bash
mvn test
```
