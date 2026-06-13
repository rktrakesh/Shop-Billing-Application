CREATE TABLE item_returns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    invoice_item_id BIGINT NOT NULL,
    product_variant_id BIGINT,
    quantity INT NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(255),
    returned_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_return_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_return_invoice_item FOREIGN KEY (invoice_item_id) REFERENCES invoice_items(id),
    CONSTRAINT fk_return_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    CONSTRAINT fk_return_user FOREIGN KEY (returned_by) REFERENCES users(id),

    INDEX idx_return_invoice (invoice_id),
    INDEX idx_return_created_at (created_at)
);