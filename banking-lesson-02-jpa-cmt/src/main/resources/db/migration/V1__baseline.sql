-- Lesson 2 baseline: customers, accounts, transfers, ledger_entries.
-- BigDecimal at precision 19 scale 2 -> NUMERIC(19,2).

CREATE TABLE IF NOT EXISTS customers (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(120) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    version     BIGINT
);

CREATE TABLE IF NOT EXISTS accounts (
    id              BIGSERIAL PRIMARY KEY,
    account_number  VARCHAR(34) NOT NULL,
    customer_id     BIGINT NOT NULL REFERENCES customers(id),
    type            VARCHAR(20) NOT NULL,
    balance         NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT,
    CONSTRAINT uk_accounts_number UNIQUE (account_number)
);

CREATE INDEX IF NOT EXISTS ix_accounts_customer ON accounts(customer_id);

CREATE TABLE IF NOT EXISTS transfers (
    id                BIGSERIAL PRIMARY KEY,
    client_request_id VARCHAR(64) NOT NULL UNIQUE,
    from_account_id   BIGINT NOT NULL REFERENCES accounts(id),
    to_account_id     BIGINT NOT NULL REFERENCES accounts(id),
    amount            NUMERIC(19,2) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason    VARCHAR(500),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMP,
    version           BIGINT
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id           BIGSERIAL PRIMARY KEY,
    account_id   BIGINT NOT NULL REFERENCES accounts(id),
    transfer_id  BIGINT NOT NULL REFERENCES transfers(id),
    direction    VARCHAR(10) NOT NULL,
    amount       NUMERIC(19,2) NOT NULL,
    posted_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_ledger_account ON ledger_entries(account_id);
CREATE INDEX IF NOT EXISTS ix_ledger_transfer ON ledger_entries(transfer_id);
