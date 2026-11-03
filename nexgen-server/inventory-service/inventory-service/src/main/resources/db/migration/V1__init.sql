CREATE TABLE inventory_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    sku_code NVARCHAR(100) NOT NULL,
    name NVARCHAR(255),
    quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_inventory_sku_code ON inventory_items(sku_code);
ALTER TABLE inventory_items ADD CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0);
ALTER TABLE inventory_items ADD CONSTRAINT chk_inventory_reserved CHECK (reserved_quantity >= 0);

CREATE TABLE inventory_reservations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_number NVARCHAR(100) NOT NULL,
    sku_code NVARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    status NVARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,
    version BIGINT
);

CREATE INDEX idx_inventory_reservations_order ON inventory_reservations(order_number);
ALTER TABLE inventory_reservations ADD CONSTRAINT chk_inventory_reservations_qty CHECK (quantity > 0);
ALTER TABLE inventory_reservations ADD CONSTRAINT chk_inventory_reservations_status CHECK (status IN ('RESERVED','RELEASED','CONFIRMED'));
