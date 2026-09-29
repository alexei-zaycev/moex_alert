CREATE TABLE tickers
(

    id         UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    name       VARCHAR(10)                 NOT NULL,
    threshold  FLOAT                       NOT NULL,
    currency   VARCHAR(3)                  NOT NULL,

    CONSTRAINT PK__TICKERS PRIMARY KEY (id),
    CONSTRAINT UQ__TICKERS__NAME UNIQUE (name),
    CONSTRAINT CHK__TICKERS__THRESHOLD CHECK (threshold > 0 AND threshold <= 100)
);

CREATE TABLE prices
(

    id         UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    ticker_id  UUID                        NOT NULL,
    ts         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    amount     DECIMAL(10, 2)              NOT NULL,
    currency   VARCHAR(3)                  NOT NULL,

    CONSTRAINT PK__PRICES PRIMARY KEY (id),
    CONSTRAINT FK__PRICES__TICKER FOREIGN KEY (ticker_id) REFERENCES tickers (id) ON DELETE CASCADE,
    CONSTRAINT CHK__PRICES__AMOUNT CHECK (amount > 0)
);
CREATE INDEX IDX__PRICES__TICKER ON prices (ticker_id, currency) INCLUDE (amount);
CREATE INDEX IDX__PRICES__TICKER_ORDER ON prices (ticker_id, ts, id) INCLUDE (amount, currency);
CREATE INDEX IDX__PRICES__ORDER ON prices (ts, id) INCLUDE (amount, currency);

CREATE TABLE alerts
(

    id              UUID                        NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,

    ticker_id       UUID                        NOT NULL,
    ts              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    amount          DECIMAL(10, 2)              NOT NULL,
    diff            FLOAT,
    currency        VARCHAR(3)                  NOT NULL,

    send_attempts   INTEGER                     NOT NULL,
    next_send_after TIMESTAMP WITHOUT TIME ZONE,
    sent_at         TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT PK__ALERTS PRIMARY KEY (id),
    CONSTRAINT FK__ALERTS__TICKERS FOREIGN KEY (ticker_id) REFERENCES tickers (id) ON DELETE CASCADE,
    CONSTRAINT CHK__ALERTS__AMOUNT CHECK (amount > 0),
    CONSTRAINT CHK__ALERTS__ATTEMPT CHECK (send_attempts >= 0)
);
CREATE INDEX IDX__ALERTS__TICKER ON alerts (ticker_id, currency) INCLUDE (amount);
CREATE INDEX IDX__ALERTS__TICKER_ORDER ON alerts (ticker_id, ts, id) INCLUDE (amount, currency);
CREATE INDEX IDX__ALERTS__ORDER ON alerts (ts, id) INCLUDE (amount, currency);
CREATE INDEX IDX__ALERTS__NOT_SENT ON alerts (next_send_after) WHERE (sent_at IS NULL);