-- V5: Add payment_mode to invoices + customer credit/outstanding tracking

-- Add payment mode to invoices (default CASH for existing records)
ALTER TABLE invoices ADD COLUMN payment_mode VARCHAR(20) NOT NULL DEFAULT 'CASH';

-- Customer credit tracking
-- Each row represents one credit-bearing invoice.
-- When a customer pays partially on a new invoice, a record is created here.
-- Repayments reduce outstanding_amount; status moves PENDING→PARTIAL→CLEARED.
CREATE TABLE IF NOT EXISTS customer_credits (
                                                id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                invoice_id          BIGINT NOT NULL UNIQUE,
                                                customer_id         BIGINT,
                                                customer_name       VARCHAR(255) NOT NULL,
    customer_mobile     VARCHAR(20),
    total_amount        DECIMAL(10,2) NOT NULL,
    amount_paid         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    outstanding_amount  DECIMAL(10,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes               VARCHAR(500),
    created_by          BIGINT NOT NULL,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6),

    CONSTRAINT fk_credit_invoice  FOREIGN KEY (invoice_id)  REFERENCES invoices(id),
    CONSTRAINT fk_credit_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_credit_user     FOREIGN KEY (created_by)  REFERENCES users(id)
    );

CREATE INDEX idx_credits_status      ON customer_credits (status);
CREATE INDEX idx_credits_customer    ON customer_credits (customer_id);
CREATE INDEX idx_credits_created_at  ON customer_credits (created_at);

-- Credit repayment history — every partial/full payment recorded separately
CREATE TABLE IF NOT EXISTS credit_payments (
                                               id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               credit_id       BIGINT NOT NULL,
                                               amount          DECIMAL(10,2) NOT NULL,
    payment_mode    VARCHAR(20) NOT NULL DEFAULT 'CASH',
    notes           VARCHAR(255),
    recorded_by     BIGINT NOT NULL,
    created_at      DATETIME(6) NOT NULL,

    CONSTRAINT fk_payment_credit FOREIGN KEY (credit_id)    REFERENCES customer_credits(id),
    CONSTRAINT fk_payment_user   FOREIGN KEY (recorded_by)  REFERENCES users(id)
    );

CREATE INDEX idx_credit_payments_credit ON credit_payments (credit_id);