CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NULL,
    role TINYINT NOT NULL COMMENT '1-admin,2-user',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-disabled,1-enabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_phone (phone),
    INDEX idx_user_role (role),
    INDEX idx_user_status (status)
);

CREATE TABLE IF NOT EXISTS store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NULL,
    contact_phone VARCHAR(20) NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-closed,1-open',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_name (name),
    INDEX idx_store_status (status)
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    description VARCHAR(512) NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-off-shelf,1-on-shelf',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_store (store_id),
    INDEX idx_product_name (name),
    INDEX idx_product_status (status),
    CONSTRAINT fk_product_store FOREIGN KEY (store_id) REFERENCES store(id)
);

CREATE TABLE IF NOT EXISTS cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cart_user_product (user_id, product_id),
    INDEX idx_cart_user (user_id),
    INDEX idx_cart_product (product_id),
    INDEX idx_cart_create_time (create_time),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS user_stats_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    cart_item_count INT NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stats_user_date (user_id, stat_date),
    INDEX idx_stats_date (stat_date),
    CONSTRAINT fk_stats_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    openid VARCHAR(128) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-unpaid,1-paid,2-closed,3-failed',
    channel VARCHAR(32) NOT NULL DEFAULT 'WECHAT_JSAPI',
    prepay_id VARCHAR(128) NULL,
    wechat_transaction_id VARCHAR(128) NULL,
    expire_time DATETIME NULL,
    pay_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_order_user_id (user_id),
    INDEX idx_payment_order_status (status),
    CONSTRAINT fk_payment_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS payment_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    cart_item_id BIGINT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    sub_total DECIMAL(10, 2) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_order_item_order_id (order_id),
    CONSTRAINT fk_payment_order_item_order FOREIGN KEY (order_id) REFERENCES payment_order(id)
);

CREATE TABLE IF NOT EXISTS payment_notify_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    notify_id VARCHAR(128) NULL,
    event_type VARCHAR(64) NULL,
    verify_status TINYINT NOT NULL DEFAULT 0,
    raw_data TEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_notify_log_order_no (order_no)
);

INSERT INTO sys_user (username, password, phone, role, status)
VALUES ('admin', '$2a$10$i0W00y9YVOlw6dFOvaU8nOFeo8P7VItG0Ilyd24fmu8JCU7YTiPg.', '13800000000', 1, 1)
ON DUPLICATE KEY UPDATE update_time = CURRENT_TIMESTAMP;

