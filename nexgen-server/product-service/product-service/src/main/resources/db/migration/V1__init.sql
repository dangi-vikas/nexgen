CREATE TABLE products (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(1000),
    price DECIMAL(19,2) NOT NULL,
    quantity INT NOT NULL,
    sku_code NVARCHAR(100) NOT NULL,
    category NVARCHAR(100),
    image_url NVARCHAR(500)
);

CREATE UNIQUE INDEX idx_products_sku_code ON products(sku_code);
ALTER TABLE products ADD CONSTRAINT chk_products_price CHECK (price > 0);
ALTER TABLE products ADD CONSTRAINT chk_products_quantity CHECK (quantity >= 0);
