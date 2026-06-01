# WEX Transaction API

A Spring Boot REST API for storing and retrieving purchase transactions with currency conversion support via the US Treasury Reporting Rates of Exchange API.

## Requirements

- Java 25
- Docker & Docker Compose (for local development with PostgreSQL)
- Gradle 8.12 (via wrapper)

## Project Structure

```
src/
├── main/
│   ├── java/com/wexinc/transaction/
│   │   ├── TransactionApplication.java
│   │   ├── client/           # External API clients
│   │   ├── config/           # Spring configuration
│   │   ├── controller/       # REST controllers
│   │   ├── domain/
│   │   │   ├── entity/       # JPA entities
│   │   │   └── model/        # DTOs and mappers
│   │   ├── exception/        # Custom exceptions & global handler
│   │   ├── repository/       # Spring Data JPA repositories
│   │   └── service/          # Business logic
│   └── resources/
│       ├── db/migration/     # Flyway migrations
│       └── application.yml
└── test/
    └── java/com/wexinc/transaction/
        ├── controller/       # Controller slice & client tests
        ├── integration/      # Full integration tests (Testcontainers)
        ├── repository/       # Repository tests (Testcontainers)
        └── service/          # Service unit tests
```

## Running Locally

### 1. Start the database

```bash
docker-compose up -d postgres
```

### 2. Run the application

```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.

### 3. Swagger UI

Navigate to `http://localhost:8080/swagger-ui.html`

## Running Tests

```bash
./gradlew test
```

Tests use Testcontainers with a real PostgreSQL container — no mocking of the database layer.

## API Endpoints

### Store a Purchase Transaction

```http
POST /api/v1/transactions
Content-Type: application/json

{
  "description": "Hotel stay",
  "transactionDate": "2024-06-15",
  "purchaseAmount": 245.50
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Hotel stay",
  "transactionDate": "2024-06-15",
  "purchaseAmount": 245.50
}
```

### Retrieve Transaction in Specified Currency

```http
GET /api/v1/transactions/{id}?country=Canada&currency=Dollar
```

**Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Hotel stay",
  "transactionDate": "2024-06-15",
  "originalAmount": 245.50,
  "exchangeRate": 1.3641,
  "convertedAmount": 334.87,
  "currency": "Dollar",
  "country": "Canada"
}
```

The `country` and `currency` parameters must match values from the [Treasury Reporting Rates of Exchange](https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange/treasury-reporting-rates-of-exchange).

## Validation Rules

| Field | Rule |
|---|---|
| `description` | Required, max 50 characters |
| `transactionDate` | Required, valid date (yyyy-MM-dd) |
| `purchaseAmount` | Required, positive, max 2 decimal places |

## Currency Conversion Rules

- Uses the Treasury Reporting Rates of Exchange API
- The exchange rate must be **≤ the purchase date** and **within the last 6 months** of the purchase date
- If no rate is available within this window, `422 Unprocessable Entity` is returned
- The converted amount is rounded to 2 decimal places (HALF_UP)

## Technology Stack

| Component | Technology |
|---|---|
| Framework | Spring Boot 3.4.x |
| Language | Java 25 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| HTTP Client | Spring WebFlux WebClient |
| Mapping | MapStruct |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Gradle 8.12 |

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `transaction_db` | Database name |
| `DB_USERNAME` | `transaction_user` | Database user |
| `DB_PASSWORD` | `transaction_pass` | Database password |
