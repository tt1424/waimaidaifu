ALTER TABLE payment_order
    MODIFY COLUMN openid VARCHAR(128) NULL;

ALTER TABLE payment_notify_log
    MODIFY COLUMN order_no VARCHAR(64) NULL;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payment_notify_log'
      AND index_name = 'uk_payment_notify_log_notify_id'
);

SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE UNIQUE INDEX uk_payment_notify_log_notify_id ON payment_notify_log (notify_id)',
    'SELECT ''skip uk_payment_notify_log_notify_id'''
);

PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
