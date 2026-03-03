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
