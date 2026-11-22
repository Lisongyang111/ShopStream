# ShopStream

ShopStream is a distributed e-commerce platform built with Spring Boot.

## Current progress

Step 1 implements product and order services, each with its own MySQL container and named volume.

The order service retrieves product prices from the product service through HTTP. It stores the price at the time of purchase and calculates the order total.

## Requirements

- Docker Desktop with the Docker engine running
- Docker Compose
- Internet access for the initial image and Maven dependency downloads
- Postman for manual API testing

The applications use Java 21 and Spring Boot 4.0.10. Their Dockerfiles build the applications from source using Maven Wrapper.

## Start the system

Run the following command from the repository root:

```shell
docker compose up --build
```

To start in the background:

```shell
docker compose up -d --build
```

Check container status and application logs:

```shell
docker compose ps
docker compose logs --tail=50 product-service order-service
```

Wait for both databases to become healthy and both applications to finish starting before sending requests.

## Services

| Service | Purpose | Host access |
|---|---|---|
| product-service | Product management | http://localhost:8080 |
| order-service | Order creation and retrieval | http://localhost:8081 |
| product-db | Product database | localhost:3308 |
| order-db | Order database | Internal Docker network |

Each service accesses its own database. The order service calls the product service at `http://product-service:8080` inside the Docker network.

## Configuration

Database connection settings are supplied through these environment variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

The order service receives the product service address through `PRODUCT_SERVICE_URL`.

Development database credentials are defined in `docker-compose.yml`.

## Product API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /products | Create a product |
| GET | /products | List products |
| GET | /products/{id} | Retrieve a product |
| PUT | /products/{id} | Update all editable product fields |
| DELETE | /products/{id} | Delete a product |

Example product request:

```json
{
  "name": "Coffee Mug",
  "description": "A ceramic mug for coffee",
  "category": "kitchen",
  "price": 19.90,
  "stock": 10
}
```

Product names, descriptions and categories must contain text. Prices and stock quantities must be non-negative. Prices support up to ten integer digits and two decimal places.

## Order API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /orders | Create an order |
| GET | /orders/{id} | Retrieve an order and its items |

The order request is a JSON array. Use an existing product ID returned by the product API.

```json
[
  {
    "productId": 2,
    "quantity": 2
  }
]
```

An order must contain at least one item. Product IDs and quantities must be positive.

The order service obtains unit prices through HTTP and saves the order and its items in one database transaction.

Stock reservation and asynchronous order confirmation will be implemented in the Kafka stage.

## Manual verification completed

- Product creation, retrieval, listing, update and deletion
- Rejection of negative product stock
- Order creation with a calculated total
- Order retrieval with its items
- Rejection of zero order quantity
- Rejection of an order containing a missing product
- Order retrieval after container removal and recreation

## Data persistence and shutdown

The `product-db-data` and `order-db-data` named volumes store MySQL data.

Stop and remove the containers and network while retaining database volumes:

```shell
docker compose down
```

Start the containers again:

```shell
docker compose up -d
```

Adding `-v` to `docker compose down` deletes the database volumes and their data.
## Step 2 - Kafka

I added Kafka in KRaft mode and a notification service.

The system uses three topics, each with three partitions:
- order-events
- inventory-events
- product-events

Order and inventory messages use the order ID as their key.
The product-events topic is ready for the search stage.

### Order processing

1. The order service saves a new order as PENDING.
2. It publishes OrderPlaced after the database transaction commits.
3. The product service checks stock and saves the reservation result
   in the same transaction as the stock changes.
4. It publishes StockReserved or StockRejected.
5. The order service updates the order to CONFIRMED or REJECTED
   and publishes the final order event.
6. The notification service listens to order and inventory events.
   It logs a simulated email for each final order status.

Stock reservation results are stored in MySQL. Receiving the same
order again returns the saved result without deducting stock again.

The notification service stores processed order IDs in its own
H2 database. A named Docker volume preserves these records.

### Run

```sh
docker compose up -d --build
```

### Manual checks

- Ordering two units changed the order to CONFIRMED.
- Product stock decreased from 10 to 8.
- Ordering 20 units was rejected because only 8 remained.
- The rejected order did not change stock.
- Replaying OrderPlaced for the confirmed order left stock at 8.
- The notification service logged confirmation and rejection emails.
- A repeated final order event was skipped by the notification service.