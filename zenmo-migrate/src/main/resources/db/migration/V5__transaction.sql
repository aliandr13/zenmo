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
