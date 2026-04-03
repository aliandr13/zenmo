# Zenmo

Multi-user spending and credit card tracker backend. REST API for accounts, categories, and transactions with JWT
authentication.

## Tech stack

- **Java 25** · **Spring Boot 4** (Web, Security, Data JPA, Validation)
- **PostgreSQL** (Flyway migrations via standalone `zenmo-migrate` module)
- **JWT** (access + refresh tokens, JJWT)
- **H2** (in-memory, tests only)

## Requirements

- JDK 25+
- Maven 3.9+
- PostgreSQL 14+ (for local run)

## Quick start

### 1. Database

Create database and user:

```sql
CREATE
USER zenmo WITH PASSWORD 'zenmo';
CREATE
DATABASE zenmo OWNER zenmo;
```

Connect to `zenmo` and allow the `pgcrypto` extension (used by Flyway):

```sql
CREATE
EXTENSION IF NOT EXISTS pgcrypto;
```

### 2. Database migrations

SQL migrations live in `zenmo-migrate/src/main/resources/db/migration/`. Apply them **before** starting the API (same
database the app uses):

```bash
mvn -pl zenmo-migrate -am package -DskipTests
java -jar zenmo-migrate/target/zenmo-migrate.jar
```

Connection defaults match the app (`jdbc:postgresql://localhost:5432/zenmo`, user/password `zenmo`). Override with
environment variables:

| Variable                    | Description |
|-----------------------------|-------------|
| `ZENMO_DATASOURCE_URL`      | JDBC URL    |
| `ZENMO_DATASOURCE_USERNAME` | DB user     |
| `ZENMO_DATASOURCE_PASSWORD` | DB password |

Or JVM system properties: `zenmo.datasource.url`, `zenmo.datasource.username`, `zenmo.datasource.password`.

### 3. Configuration

Default config is in `zenmo-app/src/main/resources/application.properties`. Override as needed (e.g. env or profile):

| Property                        | Default                                  | Description                      |
|---------------------------------|------------------------------------------|----------------------------------|
| `server.port`                   | `8080`                                   | HTTP port                        |
| `spring.datasource.url`         | `jdbc:postgresql://localhost:5432/zenmo` | DB URL                           |
| `spring.datasource.username`    | `zenmo`                                  | DB user                          |
| `spring.datasource.password`    | `zenmo`                                  | DB password                      |
| `zenmo.jwt.secret`              | *(dev value)*                            | HS256 secret (set in production) |
| `zenmo.jwt.issuer`              | `zenmo`                                  | JWT issuer                       |
| `zenmo.jwt.access-ttl-seconds`  | `900`                                    | Access token TTL                 |
| `zenmo.jwt.refresh-ttl-seconds` | `2592000`                                | Refresh token TTL                |

### 4. Run

From the repository root:

```bash
mvn -pl zenmo-app spring-boot:run
```

Or build and run the API JAR:

```bash
mvn clean package -DskipTests
java -jar zenmo-app/target/zenmo.jar
```

### 5. Tests

Tests use an in-memory H2 database (no PostgreSQL required):

```bash
mvn clean test
```

## API overview

Base URL: `http://localhost:8080` (or your `server.port`).

### Auth (public)

| Method | Path                 | Description                                                                            |
|--------|----------------------|----------------------------------------------------------------------------------------|
| `POST` | `/api/auth/register` | Register; body: `{ "email", "password" }`; returns `{ "accessToken", "refreshToken" }` |
| `POST` | `/api/auth/login`    | Login; same body/response                                                              |
| `POST` | `/api/auth/refresh`  | Body: `{ "refreshToken" }`; returns new tokens                                         |
| `GET`  | `/api/auth/me`       | Current user (requires `Authorization: Bearer <accessToken>`)                          |

### Accounts (authenticated)

| Method   | Path                 | Description                                                 |
|----------|----------------------|-------------------------------------------------------------|
| `GET`    | `/api/accounts`      | List current user's accounts                                |
| `GET`    | `/api/accounts/{id}` | Get one account                                             |
| `POST`   | `/api/accounts`      | Create account (name, type, currency, optional creditLimit) |
| `DELETE` | `/api/accounts/{id}` | Delete account                                              |

Account `type`: `CHECKING`, `CASH`, `CREDIT_CARD`.

### Categories (authenticated)

| Method   | Path                   | Description                                               |
|----------|------------------------|-----------------------------------------------------------|
| `GET`    | `/api/categories`      | List current user's categories                            |
| `GET`    | `/api/categories/{id}` | Get one category                                          |
| `POST`   | `/api/categories`      | Create category (name, optional parentId, optional color) |
| `DELETE` | `/api/categories/{id}` | Delete category                                           |

### Transactions (authenticated)

| Method   | Path                     | Description                                                                                                            |
|----------|--------------------------|------------------------------------------------------------------------------------------------------------------------|
| `GET`    | `/api/transactions`      | List transactions (optional: `accountId`, `fromDate`, `toDate`, `page`, `size`)                                        |
| `GET`    | `/api/transactions/{id}` | Get one transaction                                                                                                    |
| `POST`   | `/api/transactions`      | Create transaction (accountId, categoryId?, transactionDate, amount, currency, description, merchant?, status, notes?) |
| `DELETE` | `/api/transactions/{id}` | Delete transaction                                                                                                     |

Transaction `status`: `PENDING`, `POSTED`.

All authenticated endpoints require header: `Authorization: Bearer <accessToken>`.

## Project structure

```
src/main/java/com/github/aliandr13/zenmo/
├── ZenmoApplication.java
├── account/          # Accounts (entity, repo, service, controller, DTOs)
├── auth/             # Register, login, refresh, refresh tokens
├── category/         # Categories (entity, repo, service, controller, DTOs)
├── common/           # ApiError, ApiExceptionHandler, NotFoundException
├── security/         # JWT, AuthFacade, UserPrincipal, SecurityConfig
├── transaction/      # Transactions (Txn entity, repo, service, controller, DTOs)
└── user/             # AppUser, AppUserRepository
```

## License

Unset (see root `pom.xml`).
