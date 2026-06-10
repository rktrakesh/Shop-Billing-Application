-- ============================================================
-- V1 — Initial schema
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    full_name   VARCHAR(100),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)
);

CREATE TABLE IF NOT EXISTS shop_settings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_name       VARCHAR(100) NOT NULL,
    shop_address    VARCHAR(500),
    mobile_number   VARCHAR(20),
    email           VARCHAR(100),
    gst_number      VARCHAR(20),
    footer_message  VARCHAR(500),
    logo_path       VARCHAR(500),
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6)
);

CREATE TABLE IF NOT EXISTS products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    design_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category    VARCHAR(50),
    print_type  VARCHAR(50),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6)
);

CREATE TABLE IF NOT EXISTS product_variants (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id          BIGINT NOT NULL,
    product_code        VARCHAR(50) UNIQUE,
    color               VARCHAR(50),
    size                VARCHAR(20),
    barcode             VARCHAR(50) UNIQUE,
    selling_price       DECIMAL(10,2) NOT NULL,
    cost_price          DECIMAL(10,2) NOT NULL,
    stock               INT NOT NULL DEFAULT 0,
    minimum_stock       INT NOT NULL DEFAULT 5,
    image_url           VARCHAR(500),
    barcode_image_path  VARCHAR(500),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6),
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS customers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    mobile_number   VARCHAR(20),
    address         VARCHAR(500),
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6)
);

CREATE TABLE IF NOT EXISTS invoices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number  VARCHAR(30) NOT NULL UNIQUE,
    invoice_date    DATETIME(6) NOT NULL,
    customer_id     BIGINT,
    customer_name   VARCHAR(100),
    customer_mobile VARCHAR(20),
    subtotal        DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    grand_total     DECIMAL(10,2) NOT NULL,
    created_by      BIGINT NOT NULL,
    notes           VARCHAR(500),
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6),
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_invoice_user     FOREIGN KEY (created_by)  REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS invoice_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id          BIGINT NOT NULL,
    product_variant_id  BIGINT,
    barcode             VARCHAR(50),
    design_name         VARCHAR(100) NOT NULL,
    product_code        VARCHAR(50),
    color               VARCHAR(50),
    size                VARCHAR(20),
    print_type          VARCHAR(50),
    quantity            INT NOT NULL,
    unit_price          DECIMAL(10,2) NOT NULL,
    discount_amount     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    line_total          DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_item_invoice FOREIGN KEY (invoice_id)         REFERENCES invoices(id),
    CONSTRAINT fk_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_variant_id  BIGINT NOT NULL,
    change_type         VARCHAR(30) NOT NULL,
    quantity            INT NOT NULL,
    stock_before        INT,
    stock_after         INT,
    reason              VARCHAR(500),
    reference_id        BIGINT,
    created_by          BIGINT,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sm_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_sm_user    FOREIGN KEY (created_by)         REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS monthly_profits (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    month            INT NOT NULL,
    year             INT NOT NULL,
    total_sales      DECIMAL(12,2) NOT NULL,
    production_cost  DECIMAL(12,2) NOT NULL,
    other_expenses   DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    net_profit       DECIMAL(12,2) NOT NULL,
    notes            VARCHAR(500),
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6),
    UNIQUE KEY uq_profit_month_year (month, year)
);

CREATE TABLE IF NOT EXISTS report_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_type  VARCHAR(20) NOT NULL,
    start_date   DATE NOT NULL,
    end_date     DATE NOT NULL,
    generated_by BIGINT,
    generated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    file_path    VARCHAR(500),
    file_name    VARCHAR(200),
    CONSTRAINT fk_rh_user FOREIGN KEY (generated_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    action       VARCHAR(50) NOT NULL,
    entity_type  VARCHAR(50),
    entity_id    BIGINT,
    description  VARCHAR(1000),
    performed_by VARCHAR(50),
    ip_address   VARCHAR(50),
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_action      (action),
    INDEX idx_audit_performed_by (performed_by),
    INDEX idx_audit_created_at  (created_at)
);

-- Indexes for performance
CREATE INDEX idx_variants_barcode   ON product_variants (barcode);
CREATE INDEX idx_variants_product   ON product_variants (product_id);
CREATE INDEX idx_invoices_date      ON invoices (invoice_date);
CREATE INDEX idx_invoices_customer  ON invoices (customer_id);
CREATE INDEX idx_invoices_created_by ON invoices (created_by);
CREATE INDEX idx_stock_variant      ON stock_movements (product_variant_id);
