CREATE TABLE orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_number NVARCHAR(100) NOT NULL,
    user_id NVARCHAR(100) NOT NULL,
    status NVARCHAR(30) NOT NULL,
    created_at DATETIME2,
    total_amount DECIMAL(19,2),
    idempotency_key NVARCHAR(100),
    version BIGINT
);

CREATE UNIQUE INDEX idx_orders_order_number ON orders(order_number);
CREATE UNIQUE INDEX idx_orders_user_idempotency ON orders(user_id, idempotency_key);
ALTER TABLE orders ADD CONSTRAINT chk_orders_status CHECK (status IN ('CREATED','PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED','COMPLETED'));

CREATE TABLE order_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sku_code NVARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19,2),
    line_total DECIMAL(19,2),
    order_id BIGINT NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

ALTER TABLE order_items ADD CONSTRAINT chk_order_items_qty CHECK (quantity > 0);
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_price CHECK (price >= 0);

CREATE TABLE order_status_history (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_number NVARCHAR(100) NOT NULL,
    old_status NVARCHAR(30),
    new_status NVARCHAR(30) NOT NULL,
    changed_at DATETIME2,
    changed_by NVARCHAR(100)
);
CREATE INDEX idx_order_status_history_order ON order_status_history(order_number);
