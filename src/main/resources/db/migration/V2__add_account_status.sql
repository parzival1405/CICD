ALTER TABLE account ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
CREATE INDEX idx_account_owner ON account (owner);