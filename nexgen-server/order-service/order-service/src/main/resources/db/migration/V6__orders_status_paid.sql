-- payment-authorized moves CONFIRMED -> PAID, but the original CHECK constraint
-- (chk_orders_status) never listed PAID, so the UPDATE was rejected with SQL error
-- 547 and the event was retried until it was dead-lettered.
ALTER TABLE orders DROP CONSTRAINT chk_orders_status;

ALTER TABLE orders ADD CONSTRAINT chk_orders_status
    CHECK (status IN ('PENDING', 'CREATED', 'CONFIRMED', 'PAID', 'CANCELLED', 'DELIVERED'));
