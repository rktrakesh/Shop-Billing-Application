-- ============================================================
-- V2 — Seed Data
-- Passwords are BCrypt hashes:
--   admin123  → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
--   user123   → $2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO1GKsbske6
-- ============================================================

INSERT INTO users (username, password, role, full_name, active, created_at)
VALUES
  ('rakesh', '$2a$12$2gT4rmr.fD1SqC/SsuYaH.VetzFZgFxUOSgf06YbnVKV16IYCED0i', 'ROLE_ADMIN',   'Shop Administrator', TRUE, NOW()),
  ('user',  '$2a$12$ZNe0Zf9uIbmMcmAHSzrYJORLLR9dFSCj5cR0wYsw7sqjL8.6PvrWi', 'ROLE_USER',    'Shop User',          TRUE, NOW());

INSERT INTO shop_settings (shop_name, shop_address, mobile_number, email, gst_number, footer_message, created_at)
VALUES (
  'My Fashion Store',
  '123, Market Street, Chennai, Tamil Nadu - 600001',
  '+91 98765 43210',
  'shop@myfashionstore.com',
  '33AAAAA0000A1Z5',
  'Thank you for shopping with us! Visit again.',
  NOW()
);

-- Sample product
INSERT INTO products (design_name, description, category, print_type, active, created_at)
VALUES ('Hanuman Warrior', 'Premium mythology print oversized tee', 'Mythology', 'DTF', TRUE, NOW());

-- Sample product variant
INSERT INTO product_variants (product_id, product_code, color, size, barcode, selling_price, cost_price, stock, minimum_stock, active, created_at)
VALUES (1, 'HW-BLK-XL', 'Black', 'XL', '100001', 799.00, 350.00, 50, 5, TRUE, NOW());
