CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT
);

CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(34) NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    type VARCHAR(20) NOT NULL,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT,
    CONSTRAINT uk_accounts_number UNIQUE (account_number)
);

INSERT INTO customers(full_name, email) VALUES ('Remote Alice', 'remote.alice@example.com')
    ON CONFLICT (email) DO NOTHING;

INSERT INTO accounts(account_number, customer_id, type, balance)
    SELECT 'REMOTE-001', c.id, 'CHECKING', 500.00 FROM customers c WHERE c.email = 'remote.alice@example.com'
    ON CONFLICT (account_number) DO NOTHING;
INSERT INTO accounts(account_number, customer_id, type, balance)
    SELECT 'REMOTE-002', c.id, 'SAVINGS', 7500.00 FROM customers c WHERE c.email = 'remote.alice@example.com'
    ON CONFLICT (account_number) DO NOTHING;
