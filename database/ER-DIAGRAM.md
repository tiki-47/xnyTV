# 数据库 ER 图说明

## 实体关系图 (Entity-Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           TV Management System                              │
│                            Database ER Diagram                              │
└─────────────────────────────────────────────────────────────────────────────┘


┌───────────────────────┐           ┌──────────────────────┐
│     tv_device         │           │  tv_device_config    │
├───────────────────────┤           ├──────────────────────┤
│ PK  id                │           │ PK  id               │
│ UK  device_id*        │◄─────────┤│ UK/FK  device_id*    │
│     device_name       │    1:1    │     display_url      │
│     device_model      │           │     refresh_interval │
│     android_version   │           │     rebuild_interval │
│     app_version       │           │     enable_kiosk_mode│
│     ip_address        │           │     heartbeat_interval│
│     mac_address       │           │     ws_server_url    │
│     location          │           │     create_time      │
│     status            │           │     update_time      │
│     last_heartbeat_time│          └──────────────────────┘
│     register_time     │
│     remark            │
│     create_time       │           ┌──────────────────────┐
│     update_time       │           │  tv_command_log      │
└───────────────────────┘           ├──────────────────────┤
           │                        │ PK  id               │
           │                        │ UK  command_id       │
           │ 1                      │ FK  device_id*       │
           │                        │     command_type     │
           │                        │     command_content  │
           │ *                      │     status           │
           └───────────────────────►│     send_time        │
                                    │     ack_time         │
                                    │     result_message   │
┌───────────────────────┐           │     operator         │
│   tv_heartbeat_log    │           │     create_time      │
├───────────────────────┤           └──────────────────────┘
│ PK  id                │
│ FK  device_id*        │
│     heartbeat_time    │
│     cpu_usage         │           ┌──────────────────────┐
│     memory_usage      │           │  tv_operation_log    │
│     disk_usage        │           ├──────────────────────┤
│     network_status    │           │ PK  id               │
│     webview_status    │           │     operator         │
│     current_url       │           │     operation_type   │
│     create_time       │           │     operation_content│
└───────────────────────┘           │     target_device_id │
           ▲                        │     ip_address       │
           │                        │     user_agent       │
           │ *                      │     create_time      │
           │                        └──────────────────────┘
           │ 1                                 ▲
           │                                   │
┌───────────────────────┐                     │
│      tv_device        │                     │
│    (same as above)    │                     │
└───────────────────────┘                     │
           │                                   │
           │                                   │
           └───────────────────────────────────┘
                      relates to
                                        
                                        
┌───────────────────────┐
│       tv_user         │
├───────────────────────┤
│ PK  id                │
│ UK  username          │
│     password          │
│     real_name         │
│     phone             │
│     email             │
│     role              │
│     status            │
│     last_login_time   │
│     last_login_ip     │
│     create_time       │
│     update_time       │
└───────────────────────┘
           │
           │
           │ performs
           │
           ▼
    [operations logged in tv_operation_log]
    [commands logged in tv_command_log]
```

---

## 表关系说明

### 1. tv_device ←→ tv_device_config (一对一)

**关系类型**: 一对一 (1:1)

**外键**: `tv_device_config.device_id` → `tv_device.device_id`

**说明**:
- 每个设备有且仅有一个配置
- 设备被删除时，配置也会被级联删除 (ON DELETE CASCADE)
- 分离基础信息和配置信息，便于管理

**业务场景**:
```sql
-- 查询设备及其配置
SELECT d.*, c.display_url, c.refresh_interval
FROM tv_device d
LEFT JOIN tv_device_config c ON d.device_id = c.device_id
WHERE d.device_id = 'TV001';
```

---

### 2. tv_device ←→ tv_command_log (一对多)

**关系类型**: 一对多 (1:N)

**外键**: `tv_command_log.device_id` → `tv_device.device_id`

**说明**:
- 一个设备可以接收多条指令
- 记录所有下发给设备的指令历史
- 包含指令执行状态和结果

**业务场景**:
```sql
-- 查询设备的所有指令记录
SELECT c.command_type, c.status, c.create_time
FROM tv_command_log c
WHERE c.device_id = 'TV001'
ORDER BY c.create_time DESC;

-- 统计设备指令执行情况
SELECT 
  device_id,
  COUNT(*) as total_commands,
  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as success_count,
  SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as fail_count
FROM tv_command_log
GROUP BY device_id;
```

---

### 3. tv_device ←→ tv_heartbeat_log (一对多)

**关系类型**: 一对多 (1:N)

**外键**: `tv_heartbeat_log.device_id` → `tv_device.device_id`

**说明**:
- 一个设备产生多条心跳记录
- 记录设备健康状态的时序数据
- 建议定期清理或按月分表

**业务场景**:
```sql
-- 查询设备最近的心跳记录
SELECT *
FROM tv_heartbeat_log
WHERE device_id = 'TV001'
ORDER BY heartbeat_time DESC
LIMIT 10;

-- 统计设备平均 CPU、内存使用率
SELECT 
  device_id,
  AVG(cpu_usage) as avg_cpu,
  AVG(memory_usage) as avg_memory,
  MAX(cpu_usage) as max_cpu,
  MAX(memory_usage) as max_memory
FROM tv_heartbeat_log
WHERE heartbeat_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY device_id;
```

---

### 4. tv_device ←→ tv_operation_log (关联关系)

**关系类型**: 关联关系 (非强制外键)

**关联字段**: `tv_operation_log.target_device_id` ≈ `tv_device.device_id`

**说明**:
- 操作日志可以针对某个设备，也可以是系统级操作
- 不使用外键约束，避免删除设备时影响历史日志
- 记录管理员对设备的所有操作

**业务场景**:
```sql
-- 查询针对某设备的所有操作
SELECT *
FROM tv_operation_log
WHERE target_device_id = 'TV001'
ORDER BY create_time DESC;

-- 查询某管理员的操作历史
SELECT *
FROM tv_operation_log
WHERE operator = 'admin'
ORDER BY create_time DESC;
```

---

### 5. tv_user ←→ tv_operation_log (关联关系)

**关系类型**: 关联关系

**关联字段**: `tv_operation_log.operator` ≈ `tv_user.username`

**说明**:
- 操作日志记录操作人
- 用于审计和追溯
- 不强制外键，允许记录系统自动操作

**业务场景**:
```sql
-- 查询管理员的操作统计
SELECT 
  ol.operator,
  u.real_name,
  COUNT(*) as operation_count,
  MAX(ol.create_time) as last_operation_time
FROM tv_operation_log ol
LEFT JOIN tv_user u ON ol.operator = u.username
GROUP BY ol.operator, u.real_name
ORDER BY operation_count DESC;
```

---

### 6. tv_user ←→ tv_command_log (关联关系)

**关系类型**: 关联关系

**关联字段**: `tv_command_log.operator` ≈ `tv_user.username`

**说明**:
- 指令日志记录下发指令的操作人
- 用于追溯指令来源
- 允许系统自动下发指令(operator 为 NULL)

**业务场景**:
```sql
-- 查询管理员下发的指令
SELECT 
  c.command_type,
  c.device_id,
  c.status,
  c.create_time
FROM tv_command_log c
WHERE c.operator = 'admin'
ORDER BY c.create_time DESC;

-- 统计各管理员的指令下发情况
SELECT 
  operator,
  COUNT(*) as total,
  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as success
FROM tv_command_log
WHERE operator IS NOT NULL
GROUP BY operator;
```

---

## 索引设计说明

### 主键索引 (PRIMARY KEY)
每张表的 `id` 字段都是自增主键，用于唯一标识记录。

### 唯一索引 (UNIQUE KEY)

| 表名 | 唯一索引 | 说明 |
|------|---------|------|
| tv_device | uk_device_id | 设备ID全局唯一 |
| tv_device_config | uk_device_id | 每个设备只有一个配置 |
| tv_command_log | uk_command_id | 指令ID全局唯一 |
| tv_user | uk_username | 用户名全局唯一 |

### 普通索引 (INDEX)

| 表名 | 索引名 | 字段 | 用途 |
|------|--------|------|------|
| tv_device | idx_status | status | 按状态查询设备 |
| tv_device | idx_last_heartbeat | last_heartbeat_time | 查询心跳超时设备 |
| tv_device | idx_device_status_heartbeat | status, last_heartbeat_time | 复合查询优化 |
| tv_command_log | idx_device_id | device_id | 查询设备的指令记录 |
| tv_command_log | idx_command_type | command_type | 按指令类型统计 |
| tv_command_log | idx_status | status | 查询待执行/失败指令 |
| tv_command_log | idx_create_time | create_time | 时间范围查询 |
| tv_command_log | idx_command_device_time | device_id, create_time | 复合查询优化 |
| tv_heartbeat_log | idx_device_time | device_id, heartbeat_time | 查询设备心跳历史 |
| tv_heartbeat_log | idx_heartbeat_time | heartbeat_time | 按时间清理数据 |
| tv_operation_log | idx_operator | operator | 查询操作人记录 |
| tv_operation_log | idx_operation_type | operation_type | 按操作类型统计 |
| tv_operation_log | idx_target_device | target_device_id | 查询设备操作历史 |
| tv_operation_log | idx_create_time | create_time | 时间范围查询 |
| tv_user | idx_status | status | 查询启用的用户 |

---

## 数据完整性约束

### 外键约束 (FOREIGN KEY)

```sql
-- tv_device_config 外键
CONSTRAINT `fk_config_device` 
  FOREIGN KEY (`device_id`) 
  REFERENCES `tv_device` (`device_id`) 
  ON DELETE CASCADE
```

**说明**: 设备删除时，配置自动删除

### 字段约束

| 约束类型 | 应用场景 | 示例 |
|---------|---------|------|
| NOT NULL | 必填字段 | device_id, device_name, status |
| DEFAULT | 默认值 | status = 0, enable_kiosk_mode = 1 |
| CHECK | 枚举值 | status IN (0, 1, 2) |
| UNIQUE | 唯一性 | device_id, username |

---

## 字段类型说明

### 常用字段类型选择

| 数据类型 | 使用场景 | 示例字段 |
|---------|---------|---------|
| BIGINT | 主键、大数值 | id |
| VARCHAR(64) | 短字符串 | device_id, username |
| VARCHAR(256) | 中等字符串 | location, ws_server_url |
| VARCHAR(512) | 长字符串 | display_url, result_message |
| TEXT | 超长文本 | command_content, operation_content |
| TINYINT | 状态标志 | status, enable_kiosk_mode |
| INT | 整数 | refresh_interval, heartbeat_interval |
| DECIMAL(5,2) | 百分比 | cpu_usage(99.99%) |
| DATETIME | 时间戳 | create_time, update_time |

### 时间字段设计

```sql
-- 创建时间: 自动设置为当前时间
`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP

-- 更新时间: 自动更新为当前时间
`update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

---

## 数据字典

### tv_device.status (设备状态)
- `0` - 离线
- `1` - 在线
- `2` - 异常

### tv_command_log.status (指令状态)
- `0` - 待执行
- `1` - 已发送
- `2` - 执行成功
- `3` - 执行失败
- `4` - 超时

### tv_command_log.command_type (指令类型)
- `CHANGE_URL` - 修改展示 URL
- `REFRESH` - 刷新页面
- `REBUILD_WEBVIEW` - 重建 WebView
- `UPDATE_CONFIG` - 更新配置
- `RESTART_APP` - 重启应用

### tv_operation_log.operation_type (操作类型)
- `DEVICE_ADD` - 添加设备
- `DEVICE_UPDATE` - 更新设备
- `DEVICE_DELETE` - 删除设备
- `COMMAND_SEND` - 发送指令
- `CONFIG_UPDATE` - 更新配置

### tv_user.role (用户角色)
- `ADMIN` - 管理员(全部权限)
- `OPERATOR` - 操作员(操作设备)
- `VIEWER` - 观察者(仅查看)

### tv_user.status (用户状态)
- `0` - 禁用
- `1` - 启用

### tv_heartbeat_log.network_status (网络状态)
- `0` - 异常
- `1` - 正常

### tv_heartbeat_log.webview_status (WebView 状态)
- `0` - 异常
- `1` - 正常
- `2` - 重建中

---

## 视图说明

### v_device_status_overview (设备状态总览视图)

**用途**: 快速查看所有设备的状态概览

**字段**:
- device_id, device_name, location
- status, status_name (中文状态)
- last_heartbeat_time
- offline_seconds (离线秒数)
- display_url, refresh_interval, rebuild_interval

**使用示例**:
```sql
-- 查询所有在线设备
SELECT * FROM v_device_status_overview WHERE status = 1;

-- 查询超过5分钟未心跳的设备
SELECT * FROM v_device_status_overview WHERE offline_seconds > 300;
```

---

### v_recent_commands (最近指令视图)

**用途**: 查看最近下发的指令及执行情况

**字段**:
- command_id, device_id, device_name
- command_type, command_type_name (中文指令类型)
- status, status_name (中文状态)
- send_time, ack_time, result_message
- operator

**使用示例**:
```sql
-- 查询最近100条指令
SELECT * FROM v_recent_commands LIMIT 100;

-- 查询失败的指令
SELECT * FROM v_recent_commands WHERE status = 3;
```

---

## 存储过程说明

### sp_clean_heartbeat_log (清理心跳日志)

**用途**: 删除30天前的心跳日志

**调用方式**:
```sql
CALL sp_clean_heartbeat_log();
```

**返回**: 删除的记录数

---

### sp_clean_operation_log (清理操作日志)

**用途**: 删除90天前的操作日志

**调用方式**:
```sql
CALL sp_clean_operation_log();
```

**返回**: 删除的记录数

---

## 定时任务说明

### event_clean_history_data (定时清理历史数据)

**执行时间**: 每天凌晨 3:00

**执行内容**:
1. 清理30天前的心跳日志
2. 清理90天前的操作日志

**启用方式**:
```sql
SET GLOBAL event_scheduler = ON;
```

**查看任务状态**:
```sql
SHOW EVENTS;
SELECT * FROM information_schema.EVENTS WHERE EVENT_NAME = 'event_clean_history_data';
```

---

## 性能优化建议

### 1. 心跳日志分表策略

心跳日志数据量大，建议按月分表:

```sql
-- 创建2024年1月的心跳日志表
CREATE TABLE `tv_heartbeat_log_202401` LIKE `tv_heartbeat_log`;

-- 创建2024年2月的心跳日志表
CREATE TABLE `tv_heartbeat_log_202402` LIKE `tv_heartbeat_log`;
```

可以使用分区表自动管理:

```sql
ALTER TABLE tv_heartbeat_log
PARTITION BY RANGE (TO_DAYS(heartbeat_time)) (
  PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
  PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
  -- ...
);
```

### 2. 索引优化

根据实际查询添加复合索引:

```sql
-- 如果经常按设备和时间范围查询心跳
CREATE INDEX idx_device_time_range 
  ON tv_heartbeat_log(device_id, heartbeat_time);

-- 如果经常按状态和心跳时间查询设备
CREATE INDEX idx_status_heartbeat 
  ON tv_device(status, last_heartbeat_time);
```

### 3. 定期维护

```sql
-- 分析表统计信息
ANALYZE TABLE tv_device, tv_command_log, tv_heartbeat_log;

-- 优化表(整理碎片)
OPTIMIZE TABLE tv_device, tv_command_log, tv_heartbeat_log;

-- 检查表
CHECK TABLE tv_device;
```

---

## 备份与恢复

### 备份策略

```bash
# 全量备份
mysqldump -u root -p tv_management > tv_management_backup_$(date +%Y%m%d).sql

# 仅备份结构
mysqldump -u root -p --no-data tv_management > tv_management_schema.sql

# 仅备份数据
mysqldump -u root -p --no-create-info tv_management > tv_management_data.sql

# 备份指定表
mysqldump -u root -p tv_management tv_device tv_device_config > devices_backup.sql
```

### 恢复数据

```bash
# 恢复整个数据库
mysql -u root -p tv_management < tv_management_backup_20240101.sql

# 恢复指定表
mysql -u root -p tv_management < devices_backup.sql
```

---

## 安全建议

1. **数据库用户权限**
   ```sql
   -- 创建只读用户
   CREATE USER 'tv_readonly'@'%' IDENTIFIED BY 'password';
   GRANT SELECT ON tv_management.* TO 'tv_readonly'@'%';
   
   -- 创建应用用户(读写)
   CREATE USER 'tv_app'@'%' IDENTIFIED BY 'password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON tv_management.* TO 'tv_app'@'%';
   ```

2. **密码加密**
   - 用户密码必须使用 BCrypt 加密存储
   - 不要明文存储任何密码

3. **SQL 注入防护**
   - 使用参数化查询(PreparedStatement)
   - 不要拼接 SQL 字符串

4. **定期审计**
   - 定期检查 `tv_operation_log` 表
   - 监控异常操作行为

---

## 总结

该数据库设计具有以下特点:

✅ **结构清晰**: 职责分明，易于理解和维护
✅ **扩展性强**: 支持新增字段和表
✅ **性能优化**: 合理的索引设计
✅ **数据完整性**: 外键约束和字段约束
✅ **可追溯性**: 完整的日志记录
✅ **易于维护**: 视图、存储过程、定时任务

适用于企业内网环境的 Android TV 集中管理系统。
