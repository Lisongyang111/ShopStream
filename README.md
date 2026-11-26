# ShopStream

ShopStream is a distributed e-commerce course project built with Java 21, Spring Boot 4.0.10 and Spring Cloud 2025.1.5.

It includes product management, orders, inventory reservation, notifications, product search and a shopping cart.

## Requirements

- Docker Desktop running with Docker Compose
- Internet access for the first build
- Postman for manual API testing

## Start and Stop

Run these commands from the ShopStream repository root.

Check the configuration:

```sh
docker compose config --quiet
```

Build and start the system:

```sh
docker compose up -d --build
```

Wait for the applications to finish starting and register with Eureka before sending requests.

Check containers:

```sh
docker compose ps
```

View logs:

```sh
docker compose logs --tail=80 gateway-service
```

Stop the system while keeping its data volumes:

```sh
docker compose down
```

Do not add `-v` unless you intend to delete the stored data.

## Services

| Service | Purpose |
|---|---|
| gateway-service | Public API entry point |
| discovery-server | Eureka service registration |
| config-server | Central configuration |
| product-service | Product management and stock reservation |
| product-service-2 | Second instance of the same product application |
| order-service | Order creation and status updates |
| cart-service | Redis shopping carts and checkout |
| search-service | Elasticsearch product search |
| notification-service | Simulated email notifications |
| product-db | Product MySQL database |
| order-db | Order MySQL database |
| kafka | Event messaging in KRaft mode |
| elasticsearch | Product search index |
| redis | Shopping carts and product cache |

Only the gateway publishes a host port: `8080`.
All other services communicate through the internal Docker network.

The two product instances share the product database and Redis cache.
Both register in Eureka under the name `product-service`.

## API Address

All public requests use:

```text
http://localhost:8080/api
```

The gateway removes the `/api` prefix and forwards requests using
`lb://` service names.

### Products

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/products | Create a product |
| GET | /api/products | List products |
| GET | /api/products/{id} | Get a product |
| PUT | /api/products/{id} | Update a product |
| DELETE | /api/products/{id} | Delete a product |

Example creation request:

```json
{
  "name": "Coffee Mug",
  "description": "A ceramic mug",
  "category": "kitchen",
  "price": 19.90,
  "stock": 10
}
```

A product has an ID, name, description, category, price and stock.
Use the actual product ID returned by the API in later requests.

### Orders

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/orders | Create an order |
| GET | /api/orders/{id} | Get an order and its status |

Example order request:

```json
[
  {
    "productId": 5,
    "quantity": 1
  }
]
```

The order service obtains current product prices through Feign,
using the service name `product-service`.

A new order starts as `PENDING`. Kafka processing changes it to
`CONFIRMED` or `REJECTED`.

### Shopping Cart

| Method | Endpoint | Purpose |
|---|---|---|
| GET | /api/carts/{userId} | Get a cart |
| POST | /api/carts/{userId}/items | Add an item |
| PUT | /api/carts/{userId}/items/{productId} | Set quantity |
| DELETE | /api/carts/{userId}/items/{productId} | Remove an item |
| DELETE | /api/carts/{userId} | Empty a cart |
| POST | /api/carts/{userId}/checkout | Create an order from the cart |

Example add-item request:

```json
{
  "productId": 5,
  "quantity": 1
}
```

Example quantity update:

```json
{
  "quantity": 2
}
```

Checkout has no request body. It calls `order-service` through Feign.

- Confirmed checkout returns 201 and clears the cart.
- Rejected checkout returns 409 and keeps the items.
- Pending checkout returns 202 and keeps the items and checkout order ID.
- A later cart query checks the saved order and clears the cart if confirmed.
- A later checkout checks the saved pending order instead of creating another.
- Item changes are blocked while that checkout remains pending.

### Search

```text
GET /api/search?q=mug&category=kitchen&minPrice=10&maxPrice=30&page=0&size=10
```

Search supports text in product names and descriptions, category,
price range and pagination. Page numbering starts at 0.

The search service uses ElasticsearchRepository and
ElasticsearchOperations for combined queries.

## Kafka Processing

Kafka runs as a single broker in KRaft mode.

Three topics are declared using NewTopic and TopicBuilder.
Each topic has three partitions and one replica:

- order-events
- inventory-events
- product-events

Messages use JSON. Order and inventory events use the order ID as
their key. Product events use the product ID.

Order processing:

1. The order service saves a PENDING order and publishes OrderPlaced.
2. The product service checks all requested stock.
3. Stock changes and the reservation result are stored in one transaction.
4. The product service publishes StockReserved or StockRejected.
5. The order service updates the final status and publishes the result.
6. The notification service logs a simulated confirmation or rejection email.

Reservation results are stored by order ID. Replaying a completed
reservation returns its saved result without deducting stock again.

The notification service stores processed order IDs in its own H2
database to skip repeated final notifications.

ProductCreated, ProductUpdated and ProductDeleted events update
the Elasticsearch products index.

## Redis

Redis requires a password.

Carts use `@RedisHash("carts")`, a Redis CrudRepository and the user ID
as the key.

The cart TTL is 86400 seconds, or 24 hours, using `@TimeToLive`.
Cart updates reset the TTL. This keeps carts available for a day
and removes abandoned carts automatically.

Product lookup by ID uses `@Cacheable` with the `products` cache.
Updating or deleting a product evicts its entry.
Successful stock reservation clears the products cache.

## Configuration and Discovery

The configuration server uses the native file-system profile.
The `config-repo` directory is mounted read-only into its container.

- application.yml contains shared Kafka, Redis, Eureka and topic settings.
- product-service.yml contains product database and cache settings.
- order-service.yml contains order database settings.
- notification-service.yml contains notification database settings.
- search-service.yml contains the Elasticsearch address.
- gateway-service.yml contains the gateway routes.

Business services and the gateway load configuration using
`spring.config.import`.

Eureka registers the services. Feign uses service names for
order-to-product and cart-to-order calls.

Local development credentials are stored in the Compose and
configuration files.

## Request ID

The gateway has a custom global filter for `X-Request-Id`.

It keeps an incoming request ID or generates one when absent.
The ID is forwarded to the destination service, returned in the
response headers and written to gateway logs.

## Persistence

Named Docker volumes preserve:

- Product and order MySQL data
- Kafka data
- Elasticsearch data
- Redis data
- Notification H2 records

## Manual Checks

The following flows were tested during development:

- Product creation, retrieval, update and deletion
- Order totals and final confirmation
- Insufficient-stock rejection
- Reservation replay without a second stock deduction
- Simulated confirmation and rejection notifications
- Search synchronization, filters and pagination
- Cart item operations and 24-hour TTL countdown
- Successful checkout clearing the cart
- Rejected checkout retaining cart items
- Product cache creation and eviction
- Eureka registration with two product instances
- Configuration loading from Config Server
- Feign calls using service names
- All four gateway routes
- Matching request IDs in response headers and gateway logs
- Checkout through the gateway followed by an empty cart