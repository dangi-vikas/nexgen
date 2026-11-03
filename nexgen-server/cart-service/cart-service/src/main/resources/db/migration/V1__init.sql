CREATE TABLE cart_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id NVARCHAR(100) NOT NULL,
    product_id NVARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    version BIGINT
);

CREATE UNIQUE INDEX idx_cart_user_product ON cart_items(user_id, product_id);
ALTER TABLE cart_items ADD CONSTRAINT chk_cart_quantity CHECK (quantity > 0);
ALTER TABLE cart_items ADD CONSTRAINT chk_cart_price CHECK (price >= 0);
