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
