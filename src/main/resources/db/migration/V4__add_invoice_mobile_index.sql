-- V4: Index for walk-in customer mobile lookups (GET /api/invoices/by-mobile)
CREATE INDEX idx_invoices_customer_mobile ON invoices (customer_mobile);