# Shop Billing System — Setup Guide

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |

---

## 1. Clone / Unzip the Project

```bash
cd /path/to/shop-billing-system
```

---

## 2. Create the MySQL Database

```sql
CREATE DATABASE shop_billing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'shopbilling'@'localhost' IDENTIFIED BY 'ShopBilling@2024';
GRANT ALL PRIVILEGES ON shop_billing_db.* TO 'shopbilling'@'localhost';
FLUSH PRIVILEGES;
```

---

## 3. Configure `application.yml`

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shop_billing_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: shopbilling     # ← your DB user
    password: ShopBilling@2024  # ← your DB password
```

---

## 4. Build & Run

```bash
# Build
mvn clean install -DskipTests

# Run
mvn spring-boot:run

# Or run the JAR directly
java -jar target/shop-billing-system-1.0.0.jar
```

The app starts on **http://localhost:8080**

Flyway will automatically run migrations V1 (schema) and V2 (seed data).

---

## 5. Default Credentials

| Role    | Username | Password   |
|---------|----------|------------|
| Admin   | admin    | admin123   |
| User    | user     | user123    |

> **Change these immediately after first login via the reset-password API.**

---

## 6. API Documentation (Swagger)

Open in your browser:  
**http://localhost:8080/swagger-ui.html**

1. Click **Authorize** (top right)
2. Log in via `POST /api/auth/login` to get your JWT token
3. Paste the token as `Bearer <token>`
4. All APIs are now unlocked

---

## 7. Postman Collection

Import `Shop_Billing_Postman_Collection.json` into Postman.

1. The **Login (Admin)** request auto-saves the token to `{{token}}`
2. All other requests use `{{token}}` automatically

---

## 8. Role Permissions Summary

| Feature | Admin | Manager | User |
|---------|-------|---------|------|
| Create users/managers | ✅ | ❌ | ❌ |
| Manage products & variants | ✅ | ✅ | ❌ |
| Generate barcodes | ✅ | ✅ | ❌ |
| Manage inventory | ✅ | ✅ | ❌ |
| Create invoices | ✅ | ✅ | ✅ |
| View all invoices | ✅ | ✅ | Own only |
| View cost price | ✅ | ❌ | ❌ |
| Sales reports | ✅ | ✅ | ❌ |
| Profit reports | ✅ | ❌ | ❌ |
| Dashboard analytics | ✅ (full) | ✅ (partial) | ✅ (minimal) |
| Shop settings | ✅ | ❌ | ❌ |
| Database backup | ✅ | ❌ | ❌ |
| Audit logs | ✅ | ❌ | ❌ |

---

## 9. Invoice Creation Modes

### Barcode Mode (Scanner available)
```json
POST /api/invoices
{
  "customerName": "Walk-in Customer",
  "items": [
    { "barcode": "100001", "designName": "Hanuman Warrior", "quantity": 2, "unitPrice": 799 }
  ]
}
```
System auto-fills product details and deducts stock.

### Manual Mode (No scanner)
```json
POST /api/invoices
{
  "customerName": "Priya Sharma",
  "customerMobile": "9123456789",
  "items": [
    { "designName": "Street King", "color": "Black", "size": "L", "printType": "DTF", "unitPrice": 699, "quantity": 1, "discountAmount": 50 }
  ]
}
```

---

## 10. Barcode Workflow

```
1. Create Product    → POST /api/products
2. Create Variant    → POST /api/products/variants
3. Generate Barcode  → POST /api/barcodes/generate/{variantId}
4. Download PNG      → GET  /api/barcodes/download/png/{variantId}
5. Download PDF      → GET  /api/barcodes/download/pdf/{variantId}
6. Print & Attach to product
```

---

## 11. Report Generation

```bash
# Daily
GET /api/reports/daily

# Monthly
GET /api/reports/monthly

# Custom date range
POST /api/reports/custom
{ "startDate": "2026-01-01", "endDate": "2026-06-30" }
```
All report endpoints return a downloadable PDF.

---

## 12. Profit Management (Admin only)

```json
POST /api/profit
{
  "month": 6,
  "year": 2026,
  "totalSales": 70000,
  "productionCost": 50000,
  "otherExpenses": 2000,
  "notes": "June 2026 summary"
}
```
Net Profit = 70000 − 50000 − 2000 = **₹18,000**

---

## 13. Generated Directories

| Directory   | Purpose |
|-------------|---------|
| `./barcodes/` | Barcode PNG images |
| `./reports/`  | Generated PDF reports |
| `./backups/`  | Database backup files |
| `./uploads/`  | Shop logo & product images |
| `./logs/`     | Application log files |

---

## 14. Production / VPS Deployment

```bash
# Build production JAR
mvn clean package -DskipTests

# Run with external config
java -jar target/shop-billing-system-1.0.0.jar \
  --spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/shop_billing_db \
  --spring.datasource.username=YOUR_USER \
  --spring.datasource.password=YOUR_PASSWORD \
  --jwt.secret=YOUR_256BIT_SECRET_KEY \
  --server.port=8080
```

Or use a `application-prod.yml` profile:
```bash
java -jar shop-billing-system-1.0.0.jar --spring.profiles.active=prod
```

---

## 15. Folder Structure

```
shop-billing-system/
├── pom.xml
├── SETUP_GUIDE.md
├── Shop_Billing_Postman_Collection.json
└── src/
    └── main/
        ├── java/com/shopbilling/
        │   ├── ShopBillingApplication.java
        │   ├── config/          (SecurityConfig, OpenApiConfig, AppConfig)
        │   ├── controller/      (Auth, User, Product, Barcode, Customer,
        │   │                     Invoice, Inventory, Dashboard, Report,
        │   │                     Profit, Settings, AuditLog, Backup)
        │   ├── dto/
        │   │   ├── request/     (LoginRequest, CreateUserRequest, …)
        │   │   └── response/    (ApiResponse, AuthResponse, …)
        │   ├── entity/          (User, Product, ProductVariant, Customer,
        │   │                     Invoice, InvoiceItem, StockMovement,
        │   │                     MonthlyProfit, ReportHistory, AuditLog,
        │   │                     ShopSettings)
        │   ├── enums/           (Role, StockChangeType, ReportType, AuditAction)
        │   ├── exception/       (GlobalExceptionHandler + custom exceptions)
        │   ├── mapper/          (MapStruct mappers)
        │   ├── repository/      (Spring Data JPA repositories)
        │   ├── security/        (JwtUtil, JwtAuthFilter, UserDetailsService)
        │   ├── service/         (interfaces + impl/)
        │   └── util/            (SecurityUtils)
        └── resources/
            ├── application.yml
            └── db/migration/
                ├── V1__create_schema.sql
                └── V2__seed_data.sql
```
