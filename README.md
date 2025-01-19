MarketData and Order Management System [dwf-trading-infra]

1. Configuration
   Configfile - contains exchange specific config
   exchanges:
   auth - api secrets, account details for that exchange
   market data
   strategies

Currencies - the yml files for the contracts supported by that exchange

2. Auth Interceptor [authInterceptors/BaseInterceptor.kt]
   The BaseInterceptor class serves as an abstract base class for implementing interceptors that require HMAC signing. It uses the okhttp3.Interceptor interface, which allows modification of HTTP requests before they are sent to the server.
   Key Features:
   HMAC Signing:
   This interceptor performs cryptographic signing of the HTTP requests using HMAC algorithms (e.g., HmacSHA256).
   The HMAC computation is initialized with a secret key and a specific algorithm.
   Thread-Safe MAC Handling:
   The class uses ThreadLocal to ensure that each thread has its own instance of the Mac object, which is responsible for computing HMAC signatures.
   Helper Methods:
   getRequestBody(request: Request): Extracts the request body and returns it as a string.
   hexEncodedSignature(payload: String): Generates the HMAC signature and encodes it as a hex string.
   base64EncodedSignature(payload: String): Generates the HMAC signature and encodes it as a base64 string.
   Signature Generation:
   The signature(payload: String) method is declared abstract, meaning that each subclass must define how the signature is computed.
   Example Usage: FTXAuthInterceptor
   The FTXAuthInterceptor class extends BaseInterceptor to provide authentication for FTX trading APIs. It includes the following functionalities:
   Custom Implementation:
   The signature method uses hexEncodedSignature to compute the signature required by FTX.
   Interception Logic:
   In the intercept method, it prepares the payload from the timestamp (HEADER_TS), request method, full request path (including query parameters), and the request body.
   It then computes the HMAC signature of the payload and adds it to the request headers.

3. Configs

The folder consists of the following configuration files:

ExchangeConfig.kt:
Manages the configuration for multiple exchanges, including both market data (MD) and authenticated exchange properties.
Loads exchange configuration from YAML files and stores them in memory for use by other components in the system.
Provides utility functions to fetch specific properties from the loaded exchange configurations.
Logs configuration details using KotlinLogging.

ProxyConfig.kt:
Handles the configuration of proxies used in the trading engine.
Loads proxy configurations from YAML files and stores them in memory.
Provides utility methods to retrieve specific proxy properties and validate their existence.

TraderConfiguration.kt:
Acts as the primary configuration handler for the entire trading system.
It initializes the configurations for exchanges and proxies by invoking ExchangeConfig and ProxyConfig.
Supports additional properties, such as microLevels, which are used within the trading system.
Ensures that all required configurations are loaded from their respective YAML configuration files.

4. Contract Manager
   The ContractManager is responsible for managing the lifecycle of contracts. Contracts in this context represent the financial instruments being traded on different exchanges.
   Key features:
   Load contract configurations from a YAML file or a configuration map.
   Maintain a global currencyMap that tracks available contracts per exchange.
   Provides functions to retrieve specific contracts by their ID or symbol on a given exchange.
   Classes/Functions:
   load(configFile: String): Loads contract configurations from a YAML file.
   get(id: String): Returns a Contract by its unique ID.
   getContract(exchangeName: ExchangeName, symbol: String): Returns a Contract for a specific exchange and symbol.

5. ExchangeOverlord

ExchangeOverlord acts as a central event handler that listens to public and private exchange events. It processes incoming messages like order updates, position updates, and order book updates. The class uses a ring buffer system to manage events efficiently.
Key features:
Bind to public and private exchange listeners.
Manage the flow of data via a ring buffer and handle event dispatch using Disruptor.
Manage different types of updates, such as order book, position, wallet, and cancel rejection events.
Classes/Functions:
bind(exchangeName: ExchangeName, currency: String, listener: IPublicExchangeListener): Binds an exchange and currency to a public exchange listener.
onOrderBookUpdation(book: OrderBook): Handles order book update events.
onOrderResponse(order: Order): Processes order responses and updates.
ExchangeOverLord acts as a producer, placing new events into the ring buffer, while the consumers (event handlers) process these events in parallel.
For example:
When the order book is updated, ExchangeOverLord uses onOrderBookUpdation() to publish the update into the ring buffer.
Consumers listen for order book updates and process them as soon as they become available in the ring buffer.
This allows ExchangeOverLord to handle a high volume of updates from multiple exchanges concurrently without blocking.

6. MarketDataManager

MarketDataManager handles the subscription to market data streams from different exchanges. It manages the connection lifecycle, processes real-time order book data, and passes this data to the ExchangeOverLord.
Key features:
Initialize connections to supported exchanges (e.g., OKEX, FTX).
Subscribe to symbols for real-time market data.
Bind to IPublicExchangeListener for processing updates.
Classes/Functions:
init(): Initializes the manager by configuring exchange connections.
bind(listener: IPublicExchangeListener): Binds the ExchangeOverLord to market data updates.
subscribe(exchangeName: ExchangeName, currency: String): Subscribes to market data for a specific currency on an exchange.

7. OrderBookManager

OrderBookManager maintains the order book for multiple contracts. It stores real-time data and can reconstruct snapshots of the order book, providing the ability to access bids, asks, and trades efficiently.
Key features:
Manage the state of order books for different symbols across various exchanges.
Provides access to snapshots of order books, which can be used for trading decisions.
Classes/Functions:
Constructor: Initializes the manager with a list of contracts.
getBids(contract: Contract): Retrieves bids for a given contract.
getAsks(contract: Contract): Retrieves asks for a given contract.

8. Position Manager

The PositionManager class manages the positions of a given symbol on a specific exchange. It stores positions in a HashMap and provides functionality to retrieve and manage these positions.

9.Rate Limiter

The RateLimiter class is responsible for limiting the number of API calls that can be made within a specific time window. This helps prevent exceeding API limits imposed by exchanges.

10. SumTimed

The SumTimed class tracks the sum of values (representing calls) within a specific time window. It provides methods for adding new values, checking the current sum, and adjusting the duration of the time window.

11. WebSocketHandler

The WebSocketHandler class is responsible for handling WebSocket communication with an exchange's websocket API. It connects, subscribes to market data, and listens for messages, handling them asynchronously. It also provides reconnect logic and stale connection handling.
contracts: A set of symbols to subscribe to for market data.
bookBindings and privateBindings: These are used to bind listeners to public and private exchange data (e.g., order books, balances).
websocket: Represents the WebSocket connection to the exchange.
client: The OkHttp client used for connecting to the WebSocket.
lastMessageTime: The timestamp of the last message received, used for checking if the connection is stale.

12. Ftx folder within services [FTXOrderManager, FTXOrderStateManager]

FTXOrderManager is the main class responsible for managing FTX WebSocket connections, handling orders, and interacting with FTX’s trading and account services. It integrates WebSocket communication with REST API calls to place, cancel, and update orders.
Key Methods:
sendNewOrder(order: Order): Boolean: Places a new order on FTX after rate limiting checks and authorization.
cancelOrder(order: Order): Boolean: Cancels a specific order based on order ID.
cancelAll(symbol: String): Boolean: Cancels all orders for a specific symbol.
getPositions(): Fetches positions for the account.
getBalances(): Fetches balance information.
onOpen(webSocket: WebSocket, response: Response): WebSocket connection open handler to initiate authentication.
onMessage(webSocket: WebSocket, text: String): WebSocket message handler to process incoming messages related to orders.
WebSocket Specifics:
On connection, it sends authentication requests using API keys and signs the request for security.
It subscribes to orders and fills channels to track order and fill events.
Ping Mechanism:
Periodically sends a ping to maintain the WebSocket connection active and prevents timeouts.

FTXOrderStateManager is responsible for managing the internal state of orders placed on the FTX exchange. It handles updates received via WebSocket and REST API responses, ensuring the order state is synchronized with the exchange.
Key Methods:
receiveNewOrderUpdate(response: FTXResult<FTXOrder>): Order?: Updates the order state when a new order is created on the exchange.
receiveCancelOrderUpdate(response: FTXResult<String>, id: String): Order?: Updates the state of an order when it is canceled.
receiveOrderUpdate(data: Map<_, _>): Order?: Updates the order state based on live updates from the exchange.
receivePositionUpdate(response: FTXResult<List<FTXPosition>>, exchangeId: String): List<Position>?: Updates the positions based on the response from the account service.
receiveWalletUpdate(response: FTXResult<List<Map<String, Any>>>, exchangeId: String): List<Position>?: Updates the wallet balances of the account.

13. WebSocket service for FTX [src/main/kotlin/services/ftx/FTX.kt]

This file is responsible for managing WebSocket connections to the FTX exchange to receive real-time updates for trading data, such as order books, trades, and tickers. It interacts with the OrderBookManager to process and manage order book data for specific contracts.
Components
FTX: The main class for handling the WebSocket connection to FTX. It subscribes to the relevant WebSocket channels (such as "orderbook" and "trades") and processes incoming data.
FTXParser: A utility to parse the incoming WebSocket messages into structured data.
OrderBookManager: Manages the order book data for various contracts, updating them as new data is received.
helpers.Helpers: Utility functions for JSON serialization and other operations.
Key Classes
FTX
This class establishes a WebSocket connection to FTX and subscribes to channels to receive real-time data. It processes the data from the WebSocket messages, specifically for order books and trades.
Key Methods
onOpen: Handles the connection establishment and subscribes to the appropriate WebSocket channels.
onMessage: Processes incoming messages from the WebSocket, triggering the relevant processing functions like onOrderBook or onTrade.
onOrderBook: Processes order book snapshots and updates, using the FTXParser to parse the data and updating the OrderBookManager accordingly.
ping: Sends a ping message to the WebSocket server to keep the connection alive.
beforeConnect: Placeholder method for any actions that need to be taken before establishing the WebSocket connection.
OrderBookManager
Manages the order book data for contracts, handling both snapshot and update messages.
Key Methods
onSnapshot: Updates the order book with the latest snapshot data for a contract.
onUpdates: Applies updates to the order book when partial data is received.
orderBooks: Holds the order book data for each subscribed contract.
