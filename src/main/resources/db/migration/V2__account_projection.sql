
CREATE TABLE account_cash (
    user_id integer PRIMARY KEY REFERENCES users (id),
    settled_cash numeric(18, 2) NOT NULL,
    reserved_cash numeric(18, 2) NOT NULL
);

CREATE TABLE positions (
    user_id integer NOT NULL REFERENCES users (id),
    instrument_id integer NOT NULL REFERENCES instruments (id),
    quantity integer NOT NULL,
    cost_basis numeric(18, 8) NOT NULL,
    reserved_quantity integer NOT NULL,
    PRIMARY KEY (user_id, instrument_id)
);

CREATE INDEX orders_account_idx
    ON orders (userid, status, instrumentid)
    INCLUDE (side, type, size, price, datetime);

CREATE INDEX marketdata_latest_idx
    ON marketdata (instrumentid, date DESC)
    INCLUDE (close, previousclose);
