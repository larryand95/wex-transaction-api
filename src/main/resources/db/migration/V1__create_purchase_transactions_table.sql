-- V1__create_purchase_transactions_table.sql
-- Initial schema: purchase transactions table

CREATE TABLE purchase_transactions (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    description      VARCHAR(50)  NOT NULL,
    transaction_date DATE         NOT NULL,
    purchase_amount  NUMERIC(20, 2) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_purchase_transactions PRIMARY KEY (id),
    CONSTRAINT chk_purchase_amount_positive CHECK (purchase_amount > 0),
    CONSTRAINT chk_description_not_blank CHECK (char_length(trim(description)) > 0)
);

CREATE INDEX idx_purchase_transactions_date ON purchase_transactions (transaction_date);
CREATE INDEX idx_purchase_transactions_created_at ON purchase_transactions (created_at);

COMMENT ON TABLE purchase_transactions IS 'Stores purchase transactions with USD amounts';
COMMENT ON COLUMN purchase_transactions.id IS 'Unique identifier (UUID v4)';
COMMENT ON COLUMN purchase_transactions.description IS 'Transaction description, max 50 characters';
COMMENT ON COLUMN purchase_transactions.transaction_date IS 'Date of the purchase transaction';
COMMENT ON COLUMN purchase_transactions.purchase_amount IS 'Purchase amount in USD, rounded to nearest cent';
COMMENT ON COLUMN purchase_transactions.created_at IS 'Timestamp when the record was created';
