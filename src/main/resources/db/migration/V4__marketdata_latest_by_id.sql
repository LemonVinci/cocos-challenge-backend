DROP INDEX orders_account_idx;

DROP INDEX marketdata_latest_idx;

CREATE INDEX marketdata_latest_idx
    ON marketdata (instrumentid, date, id DESC)
    INCLUDE (close, previousclose);
