-- Test data script for local integration testing.
-- This version avoids ON DUPLICATE KEY UPDATE for broad client compatibility.

SET @demo_pwd = '$2a$10$i0W00y9YVOlw6dFOvaU8nOFeo8P7VItG0Ilyd24fmu8JCU7YTiPg.';

-- 0) Clean old demo data (safe to re-run)
DELETE FROM user_stats_daily
WHERE user_id IN (
    SELECT id
    FROM (
        SELECT id FROM sys_user WHERE username IN ('demo_admin', 'demo_user_1', 'demo_user_2')
    ) t
);

DELETE FROM cart_item
WHERE user_id IN (
    SELECT id
    FROM (
        SELECT id FROM sys_user WHERE username IN ('demo_admin', 'demo_user_1', 'demo_user_2')
    ) t
);

DELETE FROM product
WHERE name IN ('demo_product_a1', 'demo_product_a2', 'demo_product_b1');

DELETE FROM store
WHERE name IN ('demo_store_a', 'demo_store_b');

DELETE FROM sys_user
WHERE username IN ('demo_admin', 'demo_user_1', 'demo_user_2');

-- 1) Users (password: 123456)
INSERT INTO sys_user (username, password, phone, role, status)
VALUES
    ('demo_admin', @demo_pwd, '13810000001', 1, 1),
    ('demo_user_1', @demo_pwd, '13910000001', 2, 1),
    ('demo_user_2', @demo_pwd, '13910000002', 2, 1);

-- 2) Stores
INSERT INTO store (name, contact_name, contact_phone, status)
VALUES
    ('demo_store_a', 'contact_a', '13610000001', 1),
    ('demo_store_b', 'contact_b', '13610000002', 1);

-- 3) Products
INSERT INTO product (store_id, name, price, stock, description, status)
SELECT s.id, 'demo_product_a1', 19.90, 200, 'demo product a1', 1
FROM store s
WHERE s.name = 'demo_store_a';

INSERT INTO product (store_id, name, price, stock, description, status)
SELECT s.id, 'demo_product_a2', 39.90, 120, 'demo product a2', 1
FROM store s
WHERE s.name = 'demo_store_a';

INSERT INTO product (store_id, name, price, stock, description, status)
SELECT s.id, 'demo_product_b1', 59.90, 80, 'demo product b1', 1
FROM store s
WHERE s.name = 'demo_store_b';

-- 4) Cart items
INSERT INTO cart_item (user_id, product_id, quantity, unit_price, total_amount, create_time, update_time)
SELECT u.id, p.id, 2, p.price, p.price * 2, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()
FROM sys_user u
INNER JOIN product p ON p.name = 'demo_product_a1'
WHERE u.username = 'demo_user_1';

INSERT INTO cart_item (user_id, product_id, quantity, unit_price, total_amount, create_time, update_time)
SELECT u.id, p.id, 1, p.price, p.price * 1, NOW(), NOW()
FROM sys_user u
INNER JOIN product p ON p.name = 'demo_product_a2'
WHERE u.username = 'demo_user_1';

INSERT INTO cart_item (user_id, product_id, quantity, unit_price, total_amount, create_time, update_time)
SELECT u.id, p.id, 3, p.price, p.price * 3, NOW(), NOW()
FROM sys_user u
INNER JOIN product p ON p.name = 'demo_product_b1'
WHERE u.username = 'demo_user_2';

-- 5) Daily stats for today
INSERT INTO user_stats_daily (user_id, stat_date, cart_item_count, total_amount, create_time)
SELECT c.user_id, CURDATE(), SUM(c.quantity), SUM(c.total_amount), NOW()
FROM cart_item c
INNER JOIN sys_user u ON c.user_id = u.id
WHERE u.username IN ('demo_user_1', 'demo_user_2')
GROUP BY c.user_id;
