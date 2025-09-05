
/* ===================================================================
   Cesta Básica – Data Model (SQL Server)
   Author: ChatGPT (GPT-5 Thinking)
   Notes:
   - Replace MySQL ENUMs with CHECK constraints or lookup tables.
   - AUTO_INCREMENT -> IDENTITY(1,1)
   - TIMESTAMP(CURRENT_TIMESTAMP) -> DATETIME2(0) DEFAULT SYSDATETIME()
   - Generated column -> Computed column (PERSISTED)
   - JSON -> NVARCHAR(MAX) with CHECK (ISJSON(...)=1)
   =================================================================== */

-- Create DB if not exists
IF DB_ID(N'cesta_basica') IS NULL
BEGIN
    CREATE DATABASE cesta_basica;
END
GO

USE cesta_basica;
GO

/* =======================================================
   Helper lookup tables (replacing MySQL ENUM tables)
   ======================================================= */
--IF OBJECT_ID(N'dbo.enum_sources', 'U') IS NULL
--BEGIN
--    CREATE TABLE dbo.enum_sources (
--        code VARCHAR(20) NOT NULL CONSTRAINT PK_enum_sources PRIMARY KEY,
--        description VARCHAR(60) NOT NULL
--    );
--END
--GO

--IF NOT EXISTS (SELECT 1 FROM dbo.enum_sources WHERE code = 'SCRAPED')
--    INSERT INTO dbo.enum_sources(code, description) VALUES ('SCRAPED', 'Collected via web scraping');
--IF NOT EXISTS (SELECT 1 FROM dbo.enum_sources WHERE code = 'API')
--    INSERT INTO dbo.enum_sources(code, description) VALUES ('API', 'Imported via API');
--IF NOT EXISTS (SELECT 1 FROM dbo.enum_sources WHERE code = 'MANUAL')
--    INSERT INTO dbo.enum_sources(code, description) VALUES ('MANUAL', 'Entered manually');
--GO

--IF OBJECT_ID(N'dbo.enum_promo_types', 'U') IS NULL
--BEGIN
--    CREATE TABLE dbo.enum_promo_types (
--        code VARCHAR(20) NOT NULL CONSTRAINT PK_enum_promo_types PRIMARY KEY,
--        description VARCHAR(60) NOT NULL
--    );
--END
--GO

--IF NOT EXISTS (SELECT 1 FROM dbo.enum_promo_types WHERE code = 'SALE')
--    INSERT INTO dbo.enum_promo_types(code, description) VALUES ('SALE', 'Discounted price');
--IF NOT EXISTS (SELECT 1 FROM dbo.enum_promo_types WHERE code = 'BUNDLE')
--    INSERT INTO dbo.enum_promo_types(code, description) VALUES ('BUNDLE', 'Bundle/pack pricing');
--IF NOT EXISTS (SELECT 1 FROM dbo.enum_promo_types WHERE code = 'LOYALTY')
--    INSERT INTO dbo.enum_promo_types(code, description) VALUES ('LOYALTY', 'Loyalty/member price');
--IF NOT EXISTS (SELECT 1 FROM dbo.enum_promo_types WHERE code = 'COUPON')
--    INSERT INTO dbo.enum_promo_types(code, description) VALUES ('COUPON', 'Coupon-applied');
--IF NOT EXISTS (SELECT 1 FROM dbo.enum_promo_types WHERE code = 'CLEARANCE')
--    INSERT INTO dbo.enum_promo_types(code, description) VALUES ('CLEARANCE', 'Clearance/limited stock');
--GO

/* =======================================================
   Users & Segments
   ======================================================= */
IF OBJECT_ID(N'dbo.users', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_users PRIMARY KEY,
        name VARCHAR(120) NOT NULL,
        email VARCHAR(160) NOT NULL CONSTRAINT UQ_users_email UNIQUE,
        cpf CHAR(11) NULL CONSTRAINT UQ_users_cpf UNIQUE,
        phone VARCHAR(20) NULL,
        role VARCHAR(10) NOT NULL CONSTRAINT CK_users_role CHECK (role IN ('CONSUMER','MERCHANT','ADMIN')),
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSDATETIME(),
        updated_at DATETIME2(0) NULL
    );
END
GO

IF OBJECT_ID(N'dbo.audience_segment', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.audience_segment (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_audience_segment PRIMARY KEY,
        name VARCHAR(80) NOT NULL CONSTRAINT UQ_audience_segment_name UNIQUE,
        description VARCHAR(255) NULL
    );
END
GO

IF OBJECT_ID(N'dbo.user_segment', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.user_segment (
        user_id BIGINT NOT NULL,
        segment_id BIGINT NOT NULL,
        assigned_at DATETIME2(0) NOT NULL CONSTRAINT DF_user_segment_assigned_at DEFAULT SYSDATETIME(),
        CONSTRAINT PK_user_segment PRIMARY KEY (user_id, segment_id),
        CONSTRAINT FK_user_segment_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_user_segment_segment FOREIGN KEY (segment_id) REFERENCES dbo.audience_segment(id)
    );
END
GO

/* =======================================================
   Merchants (stores / markets)
   ======================================================= */
IF OBJECT_ID(N'dbo.merchants', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.merchants (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_merchants PRIMARY KEY,
        name VARCHAR(140) NOT NULL,
        brand VARCHAR(100) NULL,
        cnpj CHAR(14) NULL CONSTRAINT UQ_merchants_cnpj UNIQUE,
        document VARCHAR(30) NULL,
        phone VARCHAR(20) NULL,
        email VARCHAR(160) NULL,
        website VARCHAR(255) NULL,
        address VARCHAR(160) NULL,
        neighborhood VARCHAR(80) NULL,
        city VARCHAR(80) NOT NULL,
        state CHAR(2) NOT NULL,
        zipcode VARCHAR(12) NULL,
        lat DECIMAL(9,6) NULL,
        lon DECIMAL(9,6) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_merchants_created_at DEFAULT SYSDATETIME(),
        updated_at DATETIME2(0) NULL
    );
    CREATE INDEX IX_merchants_city_state ON dbo.merchants(city, state);
    CREATE INDEX IX_merchants_brand ON dbo.merchants(brand);
END
GO

/* =======================================================
   Catalog: Categories, Products
   ======================================================= */
IF OBJECT_ID(N'dbo.categories', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.categories (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_categories PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        parent_id BIGINT NULL,
        CONSTRAINT UQ_categories_name_parent UNIQUE (name, parent_id),
        CONSTRAINT FK_categories_parent FOREIGN KEY (parent_id) REFERENCES dbo.categories(id)
    );d
END
GO

IF OBJECT_ID(N'dbo.products', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.products (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_products PRIMARY KEY,
        sku VARCHAR(60) NULL,
        ean13 VARCHAR(13) NULL CONSTRAINT UQ_products_ean13 UNIQUE,
        name VARCHAR(160) NOT NULL,
        brand VARCHAR(100) NULL,
        category_id BIGINT NOT NULL,
        [unit] VARCHAR(8) NOT NULL CONSTRAINT CK_products_unit CHECK ([unit] IN ('UN','KG','G','L','ML','PACK','DZ','M2','M','OTHER')),
        package_qty DECIMAL(10,3) NULL,
        package_label VARCHAR(40) NULL,
        is_active BIT NOT NULL CONSTRAINT DF_products_is_active DEFAULT (1),
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_products_created_at DEFAULT SYSDATETIME(),
        updated_at DATETIME2(0) NULL,
        CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES dbo.categories(id)
    );
    CREATE INDEX IX_products_name ON dbo.products(name);
    CREATE INDEX IX_products_brand :CXZ*(
	ON dbo.products(brand);
END
GO

/* =======================================================
   Prices & Promotions
   ======================================================= */
IF OBJECT_ID(N'dbo.promotions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.promotions (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_promotions PRIMARY KEY,
        merchant_id BIGINT NOT NULL,
        title VARCHAR(160) NOT NULL,
        description VARCHAR(400) NULL,
        promo_type VARCHAR(20) NOT NULL,
        start_date DATE NULL,
        end_date DATE NULL,
        terms VARCHAR(400) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_promotions_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT FK_promo_merchant FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id),
        CONSTRAINT FK_promo_type FOREIGN KEY (promo_type) REFERENCES dbo.enum_promo_types(code)
    );
    CREATE INDEX IX_promo_merchant_dates ON dbo.promotions(merchant_id, start_date, end_date);
END
GO

IF OBJECT_ID(N'dbo.price_listings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.price_listings (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_price_listings PRIMARY KEY,
        product_id BIGINT NOT NULL,
        merchant_id BIGINT NOT NULL,
        price DECIMAL(12,2) NOT NULL CONSTRAINT CK_price_listings_price CHECK (price >= 0),
        currency CHAR(3) NOT NULL CONSTRAINT DF_price_listings_currency DEFAULT 'BRL',
        observed_at DATETIME2(0) NOT NULL,
        source VARCHAR(20) NOT NULL,
        source_url VARCHAR(400) NULL,
        in_stock BIT NOT NULL CONSTRAINT DF_price_listings_in_stock DEFAULT (1),
        promotion_id BIGINT NULL,
        unit_price AS (CASE WHEN package_qty IS NOT NULL AND package_qty > 0 THEN price / package_qty END) PERSISTED,
        package_qty DECIMAL(10,3) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_price_listings_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT FK_price_product FOREIGN KEY (product_id) REFERENCES dbo.products(id),
        CONSTRAINT FK_price_merchant FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id),
        CONSTRAINT FK_price_source FOREIGN KEY (source) REFERENCES dbo.enum_sources(code),
        CONSTRAINT FK_price_promotion FOREIGN KEY (promotion_id) REFERENCES dbo.promotions(id)
    );
    CREATE INDEX IX_price_product_time ON dbo.price_listings(product_id, observed_at DESC);
    CREATE INDEX IX_price_merchant_time ON dbo.price_listings(merchant_id, observed_at DESC);
    CREATE INDEX IX_price_product_merchant_time ON dbo.price_listings(product_id, merchant_id, observed_at DESC);
    CREATE INDEX IX_price_instock ON dbo.price_listings(in_stock);
END
GO

/* =======================================================
   Ratings / Reviews
   ======================================================= */
IF OBJECT_ID(N'dbo.product_reviews', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.product_reviews (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_product_reviews PRIMARY KEY,
        user_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        rating TINYINT NOT NULL CONSTRAINT CK_product_reviews_rating CHECK (rating BETWEEN 1 AND 5),
        comment VARCHAR(500) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_product_reviews_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT UQ_review_user_product UNIQUE (user_id, product_id),
        CONSTRAINT FK_review_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_review_product FOREIGN KEY (product_id) REFERENCES dbo.products(id)
    );
    CREATE INDEX IX_review_product ON dbo.product_reviews(product_id, rating);
END
GO

/* =======================================================
   Scraping / ETL lineage
   ======================================================= */
IF OBJECT_ID(N'dbo.crawl_job', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.crawl_job (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_crawl_job PRIMARY KEY,
        source_name VARCHAR(120) NOT NULL,
        started_at DATETIME2(0) NOT NULL CONSTRAINT DF_crawl_job_started_at DEFAULT SYSDATETIME(),
        finished_at DATETIME2(0) NULL,
        status VARCHAR(10) NOT NULL CONSTRAINT CK_crawl_job_status CHECK (status IN ('RUNNING','SUCCESS','FAILED','PARTIAL')),
        pages_crawled INT NOT NULL CONSTRAINT DF_crawl_job_pages DEFAULT 0,
        items_extracted INT NOT NULL CONSTRAINT DF_crawl_job_items DEFAULT 0,
        error_log NVARCHAR(MAX) NULL
    );
END
GO

IF OBJECT_ID(N'dbo.crawl_item', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.crawl_item (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_crawl_item PRIMARY KEY,
        job_id BIGINT NOT NULL,
        product_name_raw VARCHAR(200) NOT NULL,
        brand_raw VARCHAR(120) NULL,
        price_raw VARCHAR(60) NOT NULL,
        normalized_price DECIMAL(12,2) NULL,
        package_raw VARCHAR(60) NULL,
        url VARCHAR(400) NOT NULL,
        merchant_detected VARCHAR(160) NULL,
        merchant_id BIGINT NULL,
        product_id BIGINT NULL,
        scraped_at DATETIME2(0) NOT NULL CONSTRAINT DF_crawl_item_scraped_at DEFAULT SYSDATETIME(),
        notes VARCHAR(400) NULL,
        CONSTRAINT FK_crawl_item_job FOREIGN KEY (job_id) REFERENCES dbo.crawl_job(id),
        CONSTRAINT FK_crawl_item_merchant FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id),
        CONSTRAINT FK_crawl_item_product FOREIGN KEY (product_id) REFERENCES dbo.products(id)
    );
    CREATE INDEX IX_crawl_item_job ON dbo.crawl_item(job_id);
    CREATE INDEX IX_crawl_item_product ON dbo.crawl_item(product_id);
    CREATE INDEX IX_crawl_item_merchant ON dbo.crawl_item(merchant_id);
END
GO

/* =======================================================
   Baskets & Quotes
   ======================================================= */
IF OBJECT_ID(N'dbo.basket', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.basket (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_basket PRIMARY KEY,
        user_id BIGINT NULL,
        title VARCHAR(120) NULL,
        created_at DATETIME2(0) NOT NULL CONSTRAINT DF_basket_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT FK_basket_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
END
GO

IF OBJECT_ID(N'dbo.basket_item', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.basket_item (
        basket_id BIGINT NOT NULL,
        product_id BIGINT NOT NULL,
        quantity DECIMAL(12,3) NOT NULL CONSTRAINT DF_basket_item_quantity DEFAULT 1,
        CONSTRAINT PK_basket_item PRIMARY KEY (basket_id, product_id),
        CONSTRAINT FK_basket_item_basket FOREIGN KEY (basket_id) REFERENCES dbo.basket(id) ON DELETE CASCADE,
        CONSTRAINT FK_basket_item_product FOREIGN KEY (product_id) REFERENCES dbo.products(id)
    );
END
GO

IF OBJECT_ID(N'dbo.basket_quote', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.basket_quote (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_basket_quote PRIMARY KEY,
        basket_id BIGINT NOT NULL,
        merchant_id BIGINT NOT NULL,
        total_price DECIMAL(12,2) NOT NULL,
        computed_at DATETIME2(0) NOT NULL CONSTRAINT DF_basket_quote_computed_at DEFAULT SYSDATETIME(),
        details_json NVARCHAR(MAX) NULL CONSTRAINT CK_basket_quote_json CHECK (details_json IS NULL OR ISJSON(details_json) = 1),
        CONSTRAINT FK_basket_quote_basket FOREIGN KEY (basket_id) REFERENCES dbo.basket(id) ON DELETE CASCADE,
        CONSTRAINT FK_basket_quote_merchant FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id)
    );
    CREATE UNIQUE INDEX UQ_basket_quote ON dbo.basket_quote(basket_id, merchant_id, computed_at);
END
GO

/* =======================================================
   Aggregations & Views for Dashboards
   ======================================================= */
IF OBJECT_ID(N'dbo.product_merchant_stats', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.product_merchant_stats (
        product_id BIGINT NOT NULL,
        merchant_id BIGINT NOT NULL,
        avg_price DECIMAL(12,2) NOT NULL,
        min_price DECIMAL(12,2) NOT NULL,
        max_price DECIMAL(12,2) NOT NULL,
        last_price DECIMAL(12,2) NOT NULL,
        first_seen DATETIME2(0) NOT NULL,
        last_seen DATETIME2(0) NOT NULL,
        observations INT NOT NULL,
        CONSTRAINT PK_product_merchant_stats PRIMARY KEY (product_id, merchant_id),
        CONSTRAINT FK_pms_product FOREIGN KEY (product_id) REFERENCES dbo.products(id),
        CONSTRAINT FK_pms_merchant FOREIGN KEY (merchant_id) REFERENCES dbo.merchants(id)
    );
END
GO

-- Current lowest price per product (by city/state)
CREATE OR ALTER VIEW dbo.v_product_lowest_price AS
SELECT
  p.id AS product_id,
  p.name AS product_name,
  m.city,
  m.[state],
  pl.merchant_id,
  MIN(pl.price) AS lowest_price,
  MAX(pl.observed_at) AS last_seen
FROM dbo.products AS p
JOIN dbo.price_listings AS pl ON pl.product_id = p.id
JOIN dbo.merchants AS m ON m.id = pl.merchant_id
WHERE pl.in_stock = 1
GROUP BY p.id, p.name, m.city, m.[state], pl.merchant_id;
GO

-- Average rating per product
CREATE OR ALTER VIEW dbo.v_product_rating AS
SELECT
  pr.product_id,
  AVG(CAST(pr.rating AS DECIMAL(9,2))) AS avg_rating,
  COUNT(*) AS ratings_count
FROM dbo.product_reviews AS pr
GROUP BY pr.product_id;
GO

/* =======================================================
   Useful Indexes
   ======================================================= */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_price_city_state_category_time' AND object_id = OBJECT_ID('dbo.price_listings'))
BEGIN
    CREATE INDEX IX_price_city_state_category_time
      ON dbo.price_listings (observed_at DESC, product_id, merchant_id);
END
GO

/* =======================================================
   Seed example segments (optional)
   ======================================================= */
IF NOT EXISTS (SELECT 1 FROM dbo.audience_segment WHERE id = 1)
    INSERT INTO dbo.audience_segment(id, name, description) VALUES (1, 'Famílias', 'Produtos e preços voltados a famílias');
IF NOT EXISTS (SELECT 1 FROM dbo.audience_segment WHERE id = 2)
    INSERT INTO dbo.audience_segment(id, name, description) VALUES (2, 'Estudantes', 'Preferências e descontos para estudantes');
IF NOT EXISTS (SELECT 1 FROM dbo.audience_segment WHERE id = 3)
    INSERT INTO dbo.audience_segment(id, name, description) VALUES (3, 'Idosos', 'Segmento 60+ com foco em economia');
GO

/* =======================================================
   SAMPLE QUERIES (reference)
   =======================================================
-- 1) Histórico de preço de um produto em um mercado específico
SELECT TOP (100) observed_at, price
FROM dbo.price_listings
WHERE product_id = 1 AND merchant_id = 10
ORDER BY observed_at DESC;

-- 2) Menor preço atual por produto na cidade do Rio de Janeiro (RJ)
SELECT l.product_id, l.product_name, l.city, l.[state], l.merchant_id, MIN(l.lowest_price) AS lowest_price
FROM dbo.v_product_lowest_price AS l
WHERE l.city = 'Rio de Janeiro' AND l.[state] = 'RJ'
GROUP BY l.product_id, l.product_name, l.city, l.[state], l.merchant_id;

-- 3) Top produtos melhor avaliados (com pelo menos 5 avaliações)
SELECT TOP (20) p.id, p.name, r.avg_rating, r.ratings_count
FROM dbo.v_product_rating AS r
JOIN dbo.products AS p ON p.id = r.product_id
WHERE r.ratings_count >= 5
ORDER BY r.avg_rating DESC, r.ratings_count DESC;

-- 4) Produtos com grande variação nos últimos 30 dias
SELECT pl.product_id, p.name,
       MIN(pl.price) AS min_price_30d,
       MAX(pl.price) AS max_price_30d,
       (MAX(pl.price) - MIN(pl.price)) AS variation
FROM dbo.price_listings AS pl
JOIN dbo.products AS p ON p.id = pl.product_id
WHERE pl.observed_at >= DATEADD(DAY, -30, SYSDATETIME())
GROUP BY pl.product_id, p.name
ORDER BY variation DESC;
*/
