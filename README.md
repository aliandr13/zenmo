# Zenmo

Multi-user spending and credit card tracker backend. REST API for accounts, categories, and transactions with JWT
authentication.

## Tech stack

- **Java 25** · **Spring Boot 4** (Web MVC on Jetty, Security, JDBC, Validation, Actuator)
- **PostgreSQL** with plain `JdbcTemplate` repositories (no JPA/ORM — every query is hand-written SQL)
- **Flyway** migrations via the standalone `zenmo-migrate` module
- **JWT** (access + refresh tokens, JJWT)
- **H2** (in-memory, tests only)

## Requirements

- JDK 25+
- Maven 3.9+
- PostgreSQL 14+ (for local run)

## Quick start

### 1. Database

Create database and user (these match the `dev` profile defaults):

```sql
CREATE USER zenmo WITH PASSWORD 'zenmo-pass';
CREATE DATABASE zenmodb OWNER zenmo;
```

Connect to `zenmodb` and enable the `pgcrypto` extension (used for `gen_random_uuid()`):

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

### 2. Database migrations

SQL migrations live in `zenmo-migrate/src/main/resources/db/migration/`. Apply them **before** starting the API (same
database the app uses):

```bash
mvn -pl zenmo-migrate -am package -DskipTests
java -jar zenmo-migrate/target/zenmo-migrate.jar
```

Connection defaults: `jdbc:postgresql://localhost:5432/zenmodb`, user `zenmo`, password `zenmo-pass`. Override with
environment variables:

| Variable                    | Description |
|-----------------------------|-------------|
| `ZENMO_DATASOURCE_URL`      | JDBC URL    |
| `ZENMO_DATASOURCE_USERNAME` | DB user     |
| `ZENMO_DATASOURCE_PASSWORD` | DB password |

Or JVM system properties: `zenmo.datasource.url`, `zenmo.datasource.username`, `zenmo.datasource.password`.

Migrations:

| Version | Description                                   |
|---------|-----------------------------------------------|
| V1      | `app_user`                                    |
| V2      | `refresh_token`                               |
| V3      | `account`                                     |
| V4      | `category`                                    |
| V5      | `txn` (transactions)                          |
| V6      | Seed test user (`test@email.com`)             |

### 3. Configuration

Config lives in `zenmo-app/src/main/resources/`. The active profile is set by `SPRING_PROFILES_ACTIVE` and defaults
to `dev`.

| File                          | Purpose                                                                    |
|-------------------------------|----------------------------------------------------------------------------|
| `application.properties`      | Shared settings (port, Jetty, JWT TTLs, Actuator)                          |
| `application-dev.properties`  | Local PostgreSQL (`zenmodb` / `zenmo` / `zenmo-pass`), dev JWT secret      |
| `application-prod.properties` | Health-only Actuator, logs to `/app/logs`; secrets come from env variables |

Key properties:

| Property                        | Default                         | Description                         |
|---------------------------------|---------------------------------|-------------------------------------|
| `server.port`                   | `8080`                          | HTTP port                           |
| `spring.datasource.url`         | `${SPRING_DATASOURCE_URL}`      | DB URL (dev: `localhost:5432/zenmodb`) |
| `spring.datasource.username`    | `${SPRING_DATASOURCE_USERNAME}` | DB user (dev: `zenmo`)              |
| `spring.datasource.password`    | `${SPRING_DATASOURCE_PASSWORD}` | DB password (dev: `zenmo-pass`)     |
| `zenmo.jwt.secret`              | `${ZENMO_JWT_SECRET}`           | HS256 secret (**set in production**) |
| `zenmo.jwt.issuer`              | `zenmo`                         | JWT issuer                          |
| `zenmo.jwt.access-ttl-seconds`  | `900` (15 min; dev: 1 h)        | Access token TTL                    |
| `zenmo.jwt.refresh-ttl-seconds` | `604800` (7 days; dev: 8 h)     | Refresh token TTL                   |
| `zenmo.refresh-cookie.secure`   | `true` (dev: `false`)           | `Secure` flag on the refresh cookie; env `ZENMO_REFRESHCOOKIE_SECURE` |
| `zenmo.refresh-cookie.name`     | `zenmo_refresh`                 | Refresh cookie name                 |
| `zenmo.refresh-cookie.path`     | `/api/auth`                     | Refresh cookie path                 |

CORS allows `http://localhost:5173`, `http://localhost:3000`, and `https://zenmo.fyi`.

### 4. Run

From `zenmo-back/`:

```bash
mvn -pl zenmo-app spring-boot:run
```

Or build and run the API JAR:

```bash
mvn clean package -DskipTests
java -jar zenmo-app/target/zenmo.jar
```

### 5. Tests and quality checks

Tests use an in-memory H2 database (no PostgreSQL required):

```bash
mvn clean test                                               # unit tests (*Test)
mvn verify                                                   # + integration tests (*IT), JaCoCo ≥ 80% line coverage
mvn -pl zenmo-app -Dtest=AccountServiceTest test             # single test class
mvn -pl zenmo-app org.pitest:pitest-maven:mutationCoverage   # PIT mutation tests (≥ 80% strength)
mvn checkstyle:check                                         # Google Java Style
```

## API overview

Base URL: `http://localhost:8080` (or your `server.port`).

`/api/auth/**` and `/actuator/health`, `/actuator/info` are public; every other endpoint requires the header
`Authorization: Bearer <accessToken>` (`/api/auth/me` also needs the token to identify the user). Every resource is scoped to the current user.

A missing, invalid or expired access token returns **401** with a JSON error body (clients should refresh and retry);
403 means authenticated but not allowed.

Register, login and refresh return both tokens in the JSON body (native clients) **and** set the refresh token as a
cookie: `zenmo_refresh; HttpOnly; SameSite=Strict; Path=/api/auth` (+ `Secure` outside dev), used by the web UI.
The refresh token rotates on every refresh; the previous one stops working.

### Auth

| Method | Path                 | Auth | Description                                                                                    |
|--------|----------------------|------|------------------------------------------------------------------------------------------------|
| `POST` | `/api/auth/register` | No   | Body: `{ "email", "password" }` (password 8–200 chars); returns `{ "accessToken", "refreshToken" }` |
| `POST` | `/api/auth/login`    | No   | Same body/response                                                                             |
| `POST` | `/api/auth/refresh`  | No   | Body `{ "refreshToken" }` or the `zenmo_refresh` cookie (body wins); returns new tokens + cookie. 401 if missing/invalid/expired/revoked |
| `POST` | `/api/auth/logout`   | No   | Body `{ "refreshToken" }` or the cookie; revokes the token, clears the cookie; always 204        |
| `GET`  | `/api/auth/me`       | Yes  | Current user                                                                                   |

### Accounts

| Method   | Path                 | Description                     |
|----------|----------------------|---------------------------------|
| `GET`    | `/api/accounts`      | List current user's accounts    |
| `GET`    | `/api/accounts/{id}` | Get one account                 |
| `POST`   | `/api/accounts`      | Create account                  |
| `PUT`    | `/api/accounts/{id}` | Update account                  |
| `DELETE` | `/api/accounts/{id}` | Delete account                  |

Request body:

| Field              | Type    | Required | Notes                                          |
|--------------------|---------|----------|------------------------------------------------|
| `name`             | string  | Yes      | Max 200 chars; unique per user + type          |
| `type`             | enum    | Yes      | `CHECKING`, `CASH`, `SAVINGS`, `CREDIT`        |
| `currency`         | string  | Yes      | 3-letter code                                  |
| `creditLimit`      | decimal | No       |                                                |
| `currentBalance`   | decimal | No       | Defaults to `0`. For `CREDIT`, negative = owed |
| `statementBalance` | decimal | No       | Defaults to `0`                                |
| `paymentDueDay`    | integer | `CREDIT` | 1–31; required for `CREDIT`, must be empty otherwise |
| `closingDay`       | integer | `CREDIT` | 1–31; required for `CREDIT`, must be empty otherwise |

Response also includes `id`, `archived`, and `createdAt`.

### Categories

| Method   | Path                   | Description                    |
|----------|------------------------|--------------------------------|
| `GET`    | `/api/categories`      | List current user's categories |
| `GET`    | `/api/categories/{id}` | Get one category               |
| `POST`   | `/api/categories`      | Create category                |
| `DELETE` | `/api/categories/{id}` | Delete category                |

Request body: `name` (required, max 200, unique per user), `parentId` (optional, for nesting), `color` (optional,
max 50). Deleting a category deletes its child categories; transactions that used it become uncategorized.

### Transactions

| Method   | Path                     | Description                                                                               |
|----------|--------------------------|-------------------------------------------------------------------------------------------|
| `GET`    | `/api/transactions`      | Paged list. Query: `accountId`, `fromDate`, `toDate` (ISO dates), `page` (default `0`), `size` (default `20`) |
| `GET`    | `/api/transactions/{id}` | Get one transaction                                                                       |
| `POST`   | `/api/transactions`      | Create transaction                                                                        |
| `DELETE` | `/api/transactions/{id}` | Delete transaction                                                                        |

Request body:

| Field             | Type    | Required | Notes                   |
|-------------------|---------|----------|-------------------------|
| `accountId`       | UUID    | Yes      |                         |
| `categoryId`      | UUID    | No       |                         |
| `transactionDate` | date    | Yes      | `YYYY-MM-DD`            |
| `postDate`        | date    | No       | `YYYY-MM-DD`            |
| `amount`          | decimal | Yes      | Signed: negative = money out (purchase), positive = money in (income, refund) |
| `currency`        | string  | Yes      | 3-letter code; must match the account's currency (else `400`) |
| `description`     | string  | Yes      | Max 500 chars           |
| `merchant`        | string  | No       | Max 200 chars           |
| `status`          | enum    | Yes      | `PENDING`, `POSTED`     |
| `notes`           | string  | No       | Max 1000 chars          |

**Balance rules:** creating a `POSTED` transaction adds `amount` to the account's `currentBalance` (for every
account type, so a credit card purchase makes its balance more negative). Deleting a `POSTED` transaction reverses
it. `PENDING` transactions never change the balance. `statementBalance` is not touched.

### Actuator

`/actuator/health` (public). `/actuator/info` is also exposed outside the `prod` profile.

## Project structure

```
zenmo-back/
├── zenmo-app/        # Spring Boot API (Jetty)
├── zenmo-migrate/    # Standalone Flyway migration JAR + SQL migrations
├── Dockerfile        # API image
└── Dockerfile.migrate

zenmo-app/src/main/java/com/github/aliandr13/zenmo/
├── ZenmoApplication.java
├── account/          # Account entity, AccountType, DTOs
├── auth/             # AuthService, refresh tokens (+ JDBC repository), DTOs
├── category/         # Category entity, DTOs
├── transaction/      # Txn entity, TransactionStatus, TxnService, TxnJdbcRepository, DTOs
├── controller/       # REST controllers: Auth, Account, Category, Txn
├── repository/       # Account and Category repositories (interface + *JdbcRepository)
├── service/          # AccountService, CategoryService (business rules, user ownership)
├── security/         # JWT, SecurityConfig, JwtAuthenticationFilter, AuthFacade, @CurrentUser
├── user/             # AppUser + JDBC repository
├── common/           # ApiError, ApiExceptionHandler, NotFoundException
└── utils/            # Collection / String helpers
```

## License

Unset (see root `pom.xml`).
