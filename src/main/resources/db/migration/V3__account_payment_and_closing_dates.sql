ALTER TABLE account
    ADD COLUMN payment_due_day INTEGER,
    ADD COLUMN closing_day INTEGER NOT NULL DEFAULT 1;

ALTER TABLE account
    ADD CONSTRAINT account_payment_due_day_check CHECK (payment_due_day IS NULL OR (payment_due_day >= 1 AND payment_due_day <= 31)),
    ADD CONSTRAINT account_closing_day_check CHECK (closing_day >= 1 AND closing_day <= 31);
