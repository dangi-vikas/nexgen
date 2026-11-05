-- InventoryItem tracks creation and modification timestamps in @PrePersist/@PreUpdate,
-- but V1 never created the columns, so Hibernate's schema validation rejected the table.
ALTER TABLE inventory_items ADD created_at DATETIME2;
ALTER TABLE inventory_items ADD updated_at DATETIME2;
