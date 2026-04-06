CREATE TABLE account
(
    id                UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id           UUID           NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name              TEXT           NOT NULL,
    type              TEXT           NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    credit_limit      NUMERIC(19, 2),
    current_balance   NUMERIC(19, 2) NOT NULL DEFAULT 0,
    statement_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    payment_due_day   INTEGER,
    closing_day       INTEGER,
    archived          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
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

CREATE INDEX account_user_id_idx ON account (user_id);
