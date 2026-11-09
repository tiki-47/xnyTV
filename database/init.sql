-- =====================================================
-- Android TV 集中管理系统 - 数据库初始化脚本
-- 数据库: tv_management
-- MySQL版本: 8.0+
-- 字符集: utf8mb4
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `tv_management` 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `tv_management`;

-- =====================================================
-- 表1: tv_device (设备表)
-- 说明: 存储所有 Android TV 设备的基本信息
-- =====================================================
DROP TABLE IF EXISTS `tv_device`;
CREATE TABLE `tv_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备唯一标识(MAC地址/序列号)',
  `device_name` VARCHAR(128) NOT NULL COMMENT '设备名称',
  `device_model` VARCHAR(64) DEFAULT NULL COMMENT '设备型号',
  `android_version` VARCHAR(32) DEFAULT NULL COMMENT 'Android版本',
  `app_version` VARCHAR(32) DEFAULT NULL COMMENT 'APP版本',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
  `mac_address` VARCHAR(64) DEFAULT NULL COMMENT 'MAC地址',
  `location` VARCHAR(256) DEFAULT NULL COMMENT '设备位置(车间/楼层等)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '设备状态: 0-离线, 1-在线, 2-异常',
  `last_heartbeat_time` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
  `register_time` DATETIME NOT NULL COMMENT '注册时间',
  `remark` VARCHAR(512) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_last_heartbeat` (`last_heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TV设备表';

-- =====================================================
-- 表2: tv_device_config (设备配置表)
-- 说明: 存储每台设备的运行配置参数
-- =====================================================
DROP TABLE IF EXISTS `tv_device_config`;
CREATE TABLE `tv_device_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID',
  `display_url` VARCHAR(512) NOT NULL COMMENT '展示的URL',
  `refresh_interval` INT NOT NULL DEFAULT 0 COMMENT '定时刷新间隔(秒), 0表示不刷新',
  `rebuild_interval` INT NOT NULL DEFAULT 0 COMMENT '定时重建WebView间隔(秒), 0表示不重建',
  `enable_kiosk_mode` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用Kiosk模式: 0-否, 1-是',
  `enable_white_screen_check` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用白屏检测: 0-否, 1-是',
  `white_screen_check_interval` INT NOT NULL DEFAULT 60 COMMENT '白屏检测间隔(秒)',
  `heartbeat_interval` INT NOT NULL DEFAULT 30 COMMENT '心跳间隔(秒)',
  `ws_server_url` VARCHAR(256) NOT NULL COMMENT 'WebSocket服务器地址',
  `enable_auto_restart` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用异常自动重启: 0-否, 1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  CONSTRAINT `fk_config_device` FOREIGN KEY (`device_id`) REFERENCES `tv_device` (`device_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TV设备配置表';

-- =====================================================
-- 表3: tv_command_log (指令日志表)
-- 说明: 记录所有下发给设备的指令及执行状态
-- =====================================================
DROP TABLE IF EXISTS `tv_command_log`;
CREATE TABLE `tv_command_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `command_id` VARCHAR(64) NOT NULL COMMENT '指令ID(UUID)',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID',
  `command_type` VARCHAR(32) NOT NULL COMMENT '指令类型: CHANGE_URL, REFRESH, REBUILD_WEBVIEW, UPDATE_CONFIG, RESTART_APP',
  `command_content` TEXT DEFAULT NULL COMMENT '指令内容(JSON)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '执行状态: 0-待执行, 1-已发送, 2-执行成功, 3-执行失败, 4-超时',
  `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
  `ack_time` DATETIME DEFAULT NULL COMMENT '设备确认时间',
  `result_message` VARCHAR(512) DEFAULT NULL COMMENT '执行结果消息',
  `operator` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_command_id` (`command_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_command_type` (`command_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指令日志表';

-- =====================================================
-- 表4: tv_operation_log (操作日志表)
-- 说明: 记录管理员在后台的所有操作
-- =====================================================
DROP TABLE IF EXISTS `tv_operation_log`;
CREATE TABLE `tv_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
  `operation_type` VARCHAR(32) NOT NULL COMMENT '操作类型: DEVICE_ADD, DEVICE_UPDATE, DEVICE_DELETE, COMMAND_SEND, CONFIG_UPDATE',
  `operation_content` TEXT DEFAULT NULL COMMENT '操作内容描述',
  `target_device_id` VARCHAR(64) DEFAULT NULL COMMENT '目标设备ID',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
  `user_agent` VARCHAR(256) DEFAULT NULL COMMENT '用户代理',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator` (`operator`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_target_device` (`target_device_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =====================================================
-- 表5: tv_heartbeat_log (心跳日志表)
-- 说明: 记录设备心跳及健康状态(建议按月分表)
-- 注意: 数据量大时建议定期清理或归档
-- =====================================================
DROP TABLE IF EXISTS `tv_heartbeat_log`;
CREATE TABLE `tv_heartbeat_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID',
  `heartbeat_time` DATETIME NOT NULL COMMENT '心跳时间',
  `cpu_usage` DECIMAL(5,2) DEFAULT NULL COMMENT 'CPU使用率(%)',
  `memory_usage` DECIMAL(5,2) DEFAULT NULL COMMENT '内存使用率(%)',
  `disk_usage` DECIMAL(5,2) DEFAULT NULL COMMENT '磁盘使用率(%)',
  `network_status` TINYINT DEFAULT NULL COMMENT '网络状态: 0-异常, 1-正常',
  `webview_status` TINYINT DEFAULT NULL COMMENT 'WebView状态: 0-异常, 1-正常, 2-重建中',
  `current_url` VARCHAR(512) DEFAULT NULL COMMENT '当前展示的URL',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `heartbeat_time`),
  KEY `idx_heartbeat_time` (`heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='心跳日志表';

-- =====================================================
-- 表6: tv_user (管理员表)
-- 说明: 后台管理系统用户表
-- =====================================================
DROP TABLE IF EXISTS `tv_user`;
CREATE TABLE `tv_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码(加密存储)',
  `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `role` VARCHAR(32) NOT NULL DEFAULT 'OPERATOR' COMMENT '角色: ADMIN-管理员, OPERATOR-操作员, VIEWER-观察者',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员用户表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入默认管理员(密码: admin123, 需要在应用中使用BCrypt加密)
INSERT INTO `tv_user` 
  (`username`, `password`, `real_name`, `role`, `status`)
VALUES
  ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'ADMIN', 1);

-- 插入测试设备数据
INSERT INTO `tv_device` 
  (`device_id`, `device_name`, `device_model`, `android_version`, `app_version`, `location`, `status`, `register_time`)
VALUES
  ('TV001', '生产车间-1号屏', 'Xiaomi TV Box S', 'Android 11', '1.0.0', '1号车间-入口', 0, NOW()),
  ('TV002', '生产车间-2号屏', 'Xiaomi TV Box S', 'Android 11', '1.0.0', '2号车间-中央', 0, NOW()),
  ('TV003', '办公区-大屏', 'Xiaomi TV Box S', 'Android 11', '1.0.0', '办公区-前台', 0, NOW());

-- 插入设备配置
INSERT INTO `tv_device_config`
  (`device_id`, `display_url`, `refresh_interval`, `rebuild_interval`, `heartbeat_interval`, `ws_server_url`)
VALUES
  ('TV001', 'http://192.168.1.100/dashboard', 300, 3600, 30, 'ws://192.168.1.200:8080/ws'),
  ('TV002', 'http://192.168.1.100/production', 600, 7200, 30, 'ws://192.168.1.200:8080/ws'),
  ('TV003', 'http://192.168.1.100/announcement', 0, 3600, 30, 'ws://192.168.1.200:8080/ws');

-- =====================================================
-- 视图: 设备状态总览
-- =====================================================
CREATE OR REPLACE VIEW `v_device_status_overview` AS
SELECT 
  d.`device_id`,
  d.`device_name`,
  d.`location`,
  d.`status`,
  CASE d.`status`
    WHEN 0 THEN '离线'
    WHEN 1 THEN '在线'
    WHEN 2 THEN '异常'
    ELSE '未知'
  END AS `status_name`,
  d.`last_heartbeat_time`,
  TIMESTAMPDIFF(SECOND, d.`last_heartbeat_time`, NOW()) AS `offline_seconds`,
  c.`display_url`,
  c.`refresh_interval`,
  c.`rebuild_interval`
FROM `tv_device` d
LEFT JOIN `tv_device_config` c ON d.`device_id` = c.`device_id`;

-- =====================================================
-- 视图: 最近指令执行情况
-- =====================================================
CREATE OR REPLACE VIEW `v_recent_commands` AS
SELECT 
  cl.`command_id`,
  cl.`device_id`,
  d.`device_name`,
  cl.`command_type`,
  CASE cl.`command_type`
    WHEN 'CHANGE_URL' THEN '修改URL'
    WHEN 'REFRESH' THEN '刷新页面'
    WHEN 'REBUILD_WEBVIEW' THEN '重建WebView'
    WHEN 'UPDATE_CONFIG' THEN '更新配置'
    WHEN 'RESTART_APP' THEN '重启应用'
    ELSE cl.`command_type`
  END AS `command_type_name`,
  cl.`status`,
  CASE cl.`status`
    WHEN 0 THEN '待执行'
    WHEN 1 THEN '已发送'
    WHEN 2 THEN '执行成功'
    WHEN 3 THEN '执行失败'
    WHEN 4 THEN '超时'
    ELSE '未知'
  END AS `status_name`,
  cl.`send_time`,
  cl.`ack_time`,
  cl.`result_message`,
  cl.`operator`,
  cl.`create_time`
FROM `tv_command_log` cl
LEFT JOIN `tv_device` d ON cl.`device_id` = d.`device_id`
ORDER BY cl.`create_time` DESC;

-- =====================================================
-- 存储过程: 清理历史心跳数据(保留最近30天)
-- =====================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS `sp_clean_heartbeat_log`$$
CREATE PROCEDURE `sp_clean_heartbeat_log`()
BEGIN
  DECLARE deleted_count INT DEFAULT 0;
  
  DELETE FROM `tv_heartbeat_log` 
  WHERE `heartbeat_time` < DATE_SUB(NOW(), INTERVAL 30 DAY);
  
  SET deleted_count = ROW_COUNT();
  
  SELECT CONCAT('已清理 ', deleted_count, ' 条历史心跳记录') AS `result`;
END$$

DELIMITER ;

-- =====================================================
-- 存储过程: 清理历史操作日志(保留最近90天)
-- =====================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS `sp_clean_operation_log`$$
CREATE PROCEDURE `sp_clean_operation_log`()
BEGIN
  DECLARE deleted_count INT DEFAULT 0;
  
  DELETE FROM `tv_operation_log` 
  WHERE `create_time` < DATE_SUB(NOW(), INTERVAL 90 DAY);
  
  SET deleted_count = ROW_COUNT();
  
  SELECT CONCAT('已清理 ', deleted_count, ' 条历史操作日志') AS `result`;
END$$

DELIMITER ;

-- =====================================================
-- 定时事件: 自动清理历史数据(每天凌晨3点执行)
-- 注意: 需要开启事件调度器 SET GLOBAL event_scheduler = ON;
-- =====================================================
SET GLOBAL event_scheduler = ON;

DROP EVENT IF EXISTS `event_clean_history_data`;
CREATE EVENT `event_clean_history_data`
ON SCHEDULE EVERY 1 DAY
STARTS CONCAT(CURDATE() + INTERVAL 1 DAY, ' 03:00:00')
ON COMPLETION PRESERVE
ENABLE
DO BEGIN
  CALL sp_clean_heartbeat_log();
  CALL sp_clean_operation_log();
END;

-- =====================================================
-- 创建索引优化查询性能
-- =====================================================

-- 复合索引优化常用查询
CREATE INDEX `idx_device_status_heartbeat` ON `tv_device`(`status`, `last_heartbeat_time`);
CREATE INDEX `idx_command_device_time` ON `tv_command_log`(`device_id`, `create_time`);

-- =====================================================
-- 查询统计信息
-- =====================================================
SELECT '数据库初始化完成!' AS `message`;

SELECT 
  '设备总数' AS `统计项`,
  COUNT(*) AS `数量`
FROM `tv_device`
UNION ALL
SELECT 
  '在线设备',
  COUNT(*) 
FROM `tv_device` 
WHERE `status` = 1
UNION ALL
SELECT 
  '配置总数',
  COUNT(*) 
FROM `tv_device_config`;

-- =====================================================
-- 重要说明
-- =====================================================
/*
1. 密码说明:
   - 默认管理员密码: admin123 (实际密码需要在应用中使用BCrypt加密)
   - 建议首次登录后立即修改

2. WebSocket地址配置:
   - 需要根据实际服务器IP修改 ws_server_url
   - 格式: ws://服务器IP:端口/ws

3. 心跳日志清理:
   - 默认保留30天数据
   - 数据量大时建议按月分表

4. 性能优化建议:
   - 定期执行 OPTIMIZE TABLE 优化表
   - 监控慢查询日志
   - 根据实际查询场景添加索引

5. 备份建议:
   - 每天定时备份数据库
   - 重要操作前手动备份
*/
