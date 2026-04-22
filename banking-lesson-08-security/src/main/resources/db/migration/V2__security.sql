-- Elytron jdbc-realm tables. Clear-text only for the lesson (simplicity);
-- production uses bcrypt/modular-crypt in a proper password column.

CREATE TABLE IF NOT EXISTS sec_users (
    username VARCHAR(80) PRIMARY KEY,
    password VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS sec_user_roles (
    username VARCHAR(80) NOT NULL REFERENCES sec_users(username),
    role VARCHAR(80) NOT NULL,
    PRIMARY KEY (username, role)
);

-- Seed demo users: alice (teller), bob (customer), carol (auditor)
INSERT INTO sec_users(username, password) VALUES
    ('alice', 'alice123'),
    ('bob',   'bob123'),
    ('carol', 'carol123')
ON CONFLICT DO NOTHING;

INSERT INTO sec_user_roles(username, role) VALUES
    ('alice', 'TELLER'),
    ('bob',   'CUSTOMER'),
    ('carol', 'AUDITOR')
ON CONFLICT DO NOTHING;
