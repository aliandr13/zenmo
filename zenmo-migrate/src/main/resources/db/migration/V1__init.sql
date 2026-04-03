CREATE
EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_user
(
    id            UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    email         TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE account
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name            TEXT        NOT NULL,
    type            TEXT        NOT NULL,
    currency        VARCHAR(3)  NOT NULL,
    credit_limit    NUMERIC(19, 2),
    payment_due_day INTEGER,
    closing_day     INTEGER     NOT NULL DEFAULT 1,
    archived        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT account_payment_due_day_check CHECK (payment_due_day IS NULL OR (payment_due_day >= 1 AND payment_due_day <= 31)),
    CONSTRAINT account_closing_day_check CHECK (closing_day >= 1 AND closing_day <= 31)
);

CREATE INDEX account_user_id_idx ON account (user_id);

CREATE TABLE category
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name       TEXT        NOT NULL,
    parent_id  UUID REFERENCES category (id) ON DELETE CASCADE,
    color      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT category_unique_name_per_user UNIQUE (user_id, name)
);

CREATE INDEX category_user_id_idx ON category (user_id);

CREATE TABLE txn
(
    id               UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    account_id       UUID           NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    category_id      UUID           REFERENCES category (id) ON DELETE SET NULL,
    transaction_date DATE           NOT NULL,
    post_date        DATE,
    amount           NUMERIC(19, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    description      TEXT           NOT NULL,
    merchant         TEXT,
    status           TEXT           NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX txn_user_id_idx ON txn (user_id);
CREATE INDEX txn_user_id_transaction_date_idx ON txn (user_id, transaction_date DESC);
CREATE INDEX txn_account_id_idx ON txn (account_id);
