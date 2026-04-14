-- H2 schema for tests (mirrors Flyway migrations)
CREATE TABLE app_user
(
    id            VARCHAR(36) PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);

CREATE TABLE account
(
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36)  NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    credit_limit      DECIMAL(19, 2),
    current_balance   DECIMAL(19, 2) NOT NULL DEFAULT 0,
    statement_balance DECIMAL(19, 2) NOT NULL DEFAULT 0,
    payment_due_day   INT,
    closing_day INT,
    archived        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP      NOT NULL,
    CONSTRAINT account_type_check CHECK (type IN ('CHECKING', 'CASH', 'CREDIT', 'SAVINGS')),
    CONSTRAINT account_payment_due_day_check CHECK (
        (type <> 'CREDIT' AND payment_due_day IS NULL)
            OR (type = 'CREDIT' AND payment_due_day IS NOT NULL AND payment_due_day >= 1 AND payment_due_day <= 31)
        ),
    CONSTRAINT account_closing_day_check CHECK (
        (type <> 'CREDIT' AND closing_day IS NULL)
            OR (type = 'CREDIT' AND closing_day IS NOT NULL AND closing_day >= 1 AND closing_day <= 31)
        ),
    CONSTRAINT account_unique_name_type_per_user UNIQUE (user_id, name, type)
);

CREATE TABLE category
(
    id         VARCHAR(36) PRIMARY KEY,
    user_id    VARCHAR(36)  NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    parent_id  VARCHAR(36)  REFERENCES category (id) ON DELETE SET NULL,
    color      VARCHAR(50),
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT category_unique_name_per_user UNIQUE (user_id, name)
);

CREATE TABLE refresh_token
(
    id         VARCHAR(36) PRIMARY KEY,
    user_id    VARCHAR(36)  NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE txn
(
    id               VARCHAR(36) PRIMARY KEY,
    user_id          VARCHAR(36)    NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    account_id       VARCHAR(36)    NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    category_id      VARCHAR(36)    REFERENCES category (id) ON DELETE SET NULL,
    transaction_date DATE           NOT NULL,
    post_date        DATE,
    amount           DECIMAL(19, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    description      VARCHAR(500)   NOT NULL,
    merchant         VARCHAR(200),
    status           VARCHAR(50)    NOT NULL,
    notes            VARCHAR(1000),
    created_at       TIMESTAMP      NOT NULL
);
