CREATE TABLE transactions
(
    id               UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    account_id       UUID           NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    category_id      UUID           REFERENCES category (id) ON DELETE SET NULL,
    transaction_date DATE           NOT NULL,
    post_date        DATE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    amount           NUMERIC(19, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    merchant         TEXT,
    status           TEXT           NOT NULL,
    description      TEXT
);

CREATE INDEX transactions_user_id_idx ON transactions (user_id);
CREATE INDEX transactions_user_id_transaction_date_idx ON transactions (user_id, transaction_date DESC);
CREATE INDEX transactions_account_id_idx ON transactions (account_id);
