/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : daifu_manage

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 03/03/2026 20:36:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for cart_item
-- ----------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10, 2) NOT NULL,
  `total_amount` decimal(10, 2) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cart_user_product`(`user_id` ASC, `product_id` ASC) USING BTREE,
  INDEX `idx_cart_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_cart_product`(`product_id` ASC) USING BTREE,
  INDEX `idx_cart_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cart_item
-- ----------------------------
INSERT INTO `cart_item` VALUES (7, 10, 7, 2, 19.90, 39.80, '2026-03-01 11:45:22', '2026-03-02 11:45:22');
INSERT INTO `cart_item` VALUES (8, 10, 8, 1, 39.90, 39.90, '2026-03-02 11:45:22', '2026-03-02 11:45:22');
INSERT INTO `cart_item` VALUES (9, 11, 9, 3, 59.90, 179.70, '2026-03-02 11:45:22', '2026-03-02 11:45:22');
INSERT INTO `cart_item` VALUES (10, 12, 9, 20, 59.90, 1198.00, '2026-03-02 12:04:21', '2026-03-02 12:04:21');

-- ----------------------------
-- Table structure for payment_notify_log
-- ----------------------------
DROP TABLE IF EXISTS `payment_notify_log`;
CREATE TABLE `payment_notify_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `notify_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `verify_status` tinyint NOT NULL DEFAULT 0,
  `raw_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_payment_notify_log_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payment_notify_log
-- ----------------------------

-- ----------------------------
-- Table structure for payment_order
-- ----------------------------
DROP TABLE IF EXISTS `payment_order`;
CREATE TABLE `payment_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `openid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `total_amount` decimal(10, 2) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-unpaid,1-paid,2-closed,3-failed',
  `channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WECHAT_JSAPI',
  `prepay_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `wechat_transaction_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `expire_time` datetime NULL DEFAULT NULL,
  `pay_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_payment_order_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_payment_order_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_payment_order_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payment_order
-- ----------------------------
INSERT INTO `payment_order` VALUES (1, 'WX17724365065846b1870', 12, '111', 1198.00, 0, 'WECHAT_JSAPI', NULL, NULL, '2026-03-02 15:58:27', NULL, '2026-03-02 15:28:26', '2026-03-02 15:28:26');

-- ----------------------------
-- Table structure for payment_order_item
-- ----------------------------
DROP TABLE IF EXISTS `payment_order_item`;
CREATE TABLE `payment_order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `cart_item_id` bigint NULL DEFAULT NULL,
  `product_id` bigint NOT NULL,
  `product_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `price` decimal(10, 2) NOT NULL,
  `quantity` int NOT NULL,
  `sub_total` decimal(10, 2) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_payment_order_item_order_id`(`order_id` ASC) USING BTREE,
  CONSTRAINT `fk_payment_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `payment_order` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payment_order_item
-- ----------------------------
INSERT INTO `payment_order_item` VALUES (1, 1, 10, 9, 'demo_product_b1', 59.90, 20, 1198.00, '2026-03-02 15:28:26');

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `price` decimal(10, 2) NOT NULL,
  `stock` int NOT NULL DEFAULT 0,
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0-off-shelf,1-on-shelf',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_product_store`(`store_id` ASC) USING BTREE,
  INDEX `idx_product_name`(`name` ASC) USING BTREE,
  INDEX `idx_product_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_product_store` FOREIGN KEY (`store_id`) REFERENCES `store` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (7, 5, 'demo_product_a1', 19.90, 200, 'demo product a1', 1, '2026-03-02 11:45:22', '2026-03-02 11:45:22');
INSERT INTO `product` VALUES (8, 5, 'demo_product_a2', 39.90, 120, 'demo product a2', 1, '2026-03-02 11:45:22', '2026-03-02 11:45:22');
INSERT INTO `product` VALUES (9, 6, 'demo_product_b1', 59.90, 80, 'demo product b1', 1, '2026-03-02 11:45:22', '2026-03-02 11:45:22');

-- ----------------------------
-- Table structure for store
-- ----------------------------
DROP TABLE IF EXISTS `store`;
CREATE TABLE `store`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0-closed,1-open',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_name`(`name` ASC) USING BTREE,
  INDEX `idx_store_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of store
-- ----------------------------
INSERT INTO `store` VALUES (5, 'demo_store_a', 'contact_a', '13610000001', 1, '2026-03-02 11:45:22', '2026-03-02 11:45:22');
INSERT INTO `store` VALUES (6, 'demo_store_b', 'contact_b', '13610000002', 1, '2026-03-02 11:45:22', '2026-03-02 11:45:22');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role` tinyint NOT NULL COMMENT '1-admin,2-user',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0-disabled,1-enabled',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `idx_user_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_user_role`(`role` ASC) USING BTREE,
  INDEX `idx_user_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, '小天', '$2a$10$i0W00y9YVOlw6dFOvaU8nOFeo8P7VItG0Ilyd24fmu8JCU7YTiPg.', '13800000000', 1, 1, '2026-03-02 11:45:09', '2026-03-02 12:17:01');
INSERT INTO `sys_user` VALUES (9, '小王', '$2a$10$i0W00y9YVOlw6dFOvaU8nOFeo8P7VItG0Ilyd24fmu8JCU7YTiPg.', '13810000001', 1, 1, '2026-03-02 11:45:22', '2026-03-02 12:17:01');
INSERT INTO `sys_user` VALUES (10, '赵四', '$2a$10$i0W00y9YVOlw6dFOvaU8nOFeo8P7VItG0Ilyd24fmu8JCU7YTiPg.', '13910000001', 2, 1, '2026-03-02 11:45:22', '2026-03-02 12:17:01');
INSERT INTO `sys_user` VALUES (11, '小明', '$2a$10$i0W00y9YVOlw6dFOvaU8nOFeo8P7VItG0Ilyd24fmu8JCU7YTiPg.', '13910000002', 2, 1, '2026-03-02 11:45:22', '2026-03-02 12:17:01');
INSERT INTO `sys_user` VALUES (12, '小赵', '$2a$10$i0W00y9YVOlw6dFOvaU8nOFeo8P7VItG0Ilyd24fmu8JCU7YTiPg.', '17630801352', 2, 1, '2026-03-02 12:01:10', '2026-03-02 12:17:01');

-- ----------------------------
-- Table structure for user_stats_daily
-- ----------------------------
DROP TABLE IF EXISTS `user_stats_daily`;
CREATE TABLE `user_stats_daily`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `stat_date` date NOT NULL,
  `cart_item_count` int NOT NULL DEFAULT 0,
  `total_amount` decimal(12, 2) NOT NULL DEFAULT 0.00,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stats_user_date`(`user_id` ASC, `stat_date` ASC) USING BTREE,
  INDEX `idx_stats_date`(`stat_date` ASC) USING BTREE,
  CONSTRAINT `fk_stats_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_stats_daily
-- ----------------------------
INSERT INTO `user_stats_daily` VALUES (7, 10, '2026-03-02', 3, 79.70, '2026-03-02 11:45:22');
INSERT INTO `user_stats_daily` VALUES (8, 11, '2026-03-02', 3, 179.70, '2026-03-02 11:45:22');

SET FOREIGN_KEY_CHECKS = 1;
