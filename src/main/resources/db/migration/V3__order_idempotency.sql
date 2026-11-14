ALTER TABLE orders
    ADD COLUMN idempotency_key varchar(64);

CREATE UNIQUE INDEX orders_idempotency_idx
    ON orders (userid, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
