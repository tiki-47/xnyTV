# Android TV 集中管理系统 - 完整设计方案

## 一、可行性分析

### ✅ 可行性结论
**该方案完全可行**，理由如下:

#### 1. 技术栈成熟度
- **Android TV + WebView**: Android 原生支持，性能稳定
- **WebSocket**: 双向通信标准协议，适合实时控制
- **Spring Boot + MySQL**: 企业级成熟技术栈
- **内网部署**: 不依赖外网，安全可控

#### 2. 核心功能可实现性
| 功能 | 技术实现 | 可行性 |
|-----|---------|-------|
| 全屏 WebView | `WebView + FLAG_FULLSCREEN` | ✅ 原生支持 |
| WebSocket 长连接 | OkHttp3 WebSocket | ✅ 成熟方案 |
| 定时刷新 | `Handler + Timer` | ✅ 简单可靠 |
| 定时重建 WebView | 销毁后重新创建 | ✅ 防内存泄漏 |
| Kiosk 模式 | 禁用返回键 + 启动器替换 | ✅ 可实现 |
| 开机自启 | `BOOT_COMPLETED` 广播 | ✅ 需系统权限 |
| 心跳检测 | 定时上报 + 服务端超时判断 | ✅ 标准方案 |
| 远程控制 | WebSocket 下发指令 | ✅ 实时响应 |

#### 3. 潜在风险与解决方案
| 风险 | 解决方案 |
|-----|---------|
| WebView 内存泄漏 | 定时重建 + 及时释放资源 |
| 网络断线 | 自动重连 + 指数退避算法 |
| TV 异常退出 | Kiosk 模式 + 前台服务保活 |
| 白屏/卡死 | 定时检测 + 自动重启 WebView |
| 设备掉线 | 心跳超时判断 + 离线状态维护 |

---

## 二、系统架构设计

### 1. 总体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        企业内网环境                            │
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Android TV 1│  │  Android TV 2│  │  Android TV n│      │
│  │              │  │              │  │              │      │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │      │
│  │  │WebView │  │  │  │WebView │  │  │  │WebView │  │      │
│  │  │ 全屏   │  │  │  │ 全屏   │  │  │  │ 全屏   │  │      │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │      │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │      │
│  │  │WebSocket│  │  │  │WebSocket│  │  │  │WebSocket│  │      │
│  │  │ 客户端  │  │  │  │ 客户端  │  │  │  │ 客户端  │  │      │
│  │  └────┬───┘  │  │  └────┬───┘  │  │  └────┬───┘  │      │
│  └───────┼──────┘  └───────┼──────┘  └───────┼──────┘      │
│          │                 │                 │              │
│          └─────────────────┼─────────────────┘              │
│                            │                                │
│                   ┌────────▼────────┐                       │
│                   │  服务端集群      │                       │
│                   │                 │                       │
│                   │ ┌─────────────┐ │                       │
│                   │ │WebSocket 服务│ │                       │
│                   │ │连接管理/心跳 │ │                       │
│                   │ └──────┬──────┘ │                       │
│                   │        │        │                       │
│                   │ ┌──────▼──────┐ │                       │
│                   │ │ 设备管理服务 │ │                       │
│                   │ │ 指令下发服务 │ │                       │
│                   │ └──────┬──────┘ │                       │
│                   │        │        │                       │
│                   │ ┌──────▼──────┐ │                       │
│                   │ │   数据持久化 │ │                       │
│                   │ │   MySQL 8.0  │ │                       │
│                   │ └─────────────┘ │                       │
│                   │                 │                       │
│                   │ ┌─────────────┐ │                       │
│                   │ │Web 管理后台 │ │                       │
│                   │ │(HTTP API)   │ │                       │
│                   │ └─────────────┘ │                       │
│                   └─────────────────┘                       │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 2. TV 端架构设计

```kotlin
// 核心模块划分
tvclient/
├── app/
│   ├── src/main/java/com/xny/tvclient/
│   │   ├── MainActivity.kt              // 主 Activity
│   │   ├── WebViewActivity.kt           // WebView 全屏展示
│   │   ├── service/
│   │   │   ├── WebSocketService.kt      // WebSocket 服务
│   │   │   ├── KeepAliveService.kt      // 保活服务(前台服务)
│   │   │   └── HeartbeatService.kt      // 心跳上报服务
│   │   ├── receiver/
│   │   │   └── BootReceiver.kt          // 开机自启广播
│   │   ├── manager/
│   │   │   ├── WebViewManager.kt        // WebView 生命周期管理
│   │   │   ├── CommandHandler.kt        // 指令处理器
│   │   │   └── ConfigManager.kt         // 配置管理(SharedPreferences)
│   │   ├── model/
│   │   │   ├── Command.kt               // 指令数据模型
│   │   │   ├── DeviceInfo.kt            // 设备信息
│   │   │   └── Heartbeat.kt             // 心跳数据
│   │   ├── utils/
│   │   │   ├── NetworkUtil.kt           // 网络工具
│   │   │   └── DeviceUtil.kt            // 设备信息获取
│   │   └── constant/
│   │       └── Constants.kt             // 常量定义
```

#### TV 端核心功能模块

##### 1) WebView 管理模块
```kotlin
class WebViewManager {
    private var webView: WebView? = null
    private var lastRefreshTime: Long = 0
    private val refreshTimer = Timer()
    
    // 初始化 WebView
    fun initWebView(context: Context, url: String)
    
    // 刷新页面
    fun refresh()
    
    // 重建 WebView(防内存泄漏)
    fun rebuild()
    
    // 检测白屏
    fun checkWhiteScreen(): Boolean
    
    // 释放资源
    fun destroy()
}
```

##### 2) WebSocket 客户端模块
```kotlin
class WebSocketService : Service() {
    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    
    // 连接服务器
    fun connect(serverUrl: String, deviceId: String)
    
    // 断线重连(指数退避)
    fun reconnect()
    
    // 发送消息
    fun send(message: String)
    
    // 接收消息并分发
    override fun onMessage(text: String)
    
    // 心跳发送
    fun sendHeartbeat()
}
```

##### 3) 指令处理模块
```kotlin
class CommandHandler {
    fun handleCommand(command: Command) {
        when (command.type) {
            "CHANGE_URL" -> changeUrl(command.url)
            "REFRESH" -> refreshWebView()
            "RESTART_APP" -> restartApp()
            "REBUILD_WEBVIEW" -> rebuildWebView()
            "UPDATE_CONFIG" -> updateConfig(command.config)
        }
    }
}
```

##### 4) Kiosk 模式实现
```kotlin
class WebViewActivity : AppCompatActivity() {
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 禁用返回键、Home键
        if (keyCode == KeyEvent.KEYCODE_BACK || 
            keyCode == KeyEvent.KEYCODE_HOME) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // 保持全屏沉浸模式
            hideSystemUI()
        }
    }
}
```

### 3. 服务端架构设计

```
tvservice/
├── src/main/java/cn/xny/tvservice/
│   ├── TvserviceApplication.java
│   ├── config/
│   │   ├── WebSocketConfig.java         // WebSocket 配置
│   │   ├── MybatisPlusConfig.java       // MyBatis-Plus 配置
│   │   └── ScheduleConfig.java          // 定时任务配置
│   ├── controller/
│   │   ├── DeviceController.java        // 设备管理 API
│   │   ├── CommandController.java       // 指令下发 API
│   │   └── MonitorController.java       // 监控数据 API
│   ├── websocket/
│   │   ├── WebSocketHandler.java        // WebSocket 处理器
│   │   └── WebSocketSessionManager.java // Session 管理器
│   ├── service/
│   │   ├── DeviceService.java           // 设备服务
│   │   ├── CommandService.java          // 指令服务
│   │   ├── HeartbeatService.java        // 心跳处理服务
│   │   └── OperationLogService.java     // 操作日志服务
│   ├── entity/
│   │   ├── Device.java                  // 设备实体
│   │   ├── DeviceConfig.java            // 设备配置实体
│   │   ├── Command.java                 // 指令实体
│   │   └── OperationLog.java            // 操作日志实体
│   ├── mapper/
│   │   ├── DeviceMapper.java
│   │   ├── DeviceConfigMapper.java
│   │   ├── CommandMapper.java
│   │   └── OperationLogMapper.java
│   ├── dto/
│   │   ├── DeviceStatusDTO.java         // 设备状态 DTO
│   │   ├── CommandDTO.java              // 指令 DTO
│   │   └── HeartbeatDTO.java            // 心跳 DTO
│   └── scheduled/
│       └── HeartbeatCheckTask.java      // 心跳检查定时任务
```

#### 服务端核心功能模块

##### 1) WebSocket 连接管理
```java
@Component
public class WebSocketSessionManager {
    // deviceId -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // 注册设备连接
    public void registerSession(String deviceId, WebSocketSession session);
    
    // 移除设备连接
    public void removeSession(String deviceId);
    
    // 向指定设备发送消息
    public void sendToDevice(String deviceId, String message);
    
    // 广播消息
    public void broadcast(String message);
    
    // 获取在线设备列表
    public List<String> getOnlineDevices();
}
```

##### 2) 心跳检测机制
```java
@Component
public class HeartbeatService {
    // deviceId -> 最后心跳时间
    private final ConcurrentHashMap<String, Long> heartbeatMap = new ConcurrentHashMap<>();
    
    // 更新心跳时间
    public void updateHeartbeat(String deviceId);
    
    // 检查设备是否离线(超过60秒未心跳)
    public boolean isOffline(String deviceId);
    
    // 定时检查所有设备(每30秒)
    @Scheduled(fixedRate = 30000)
    public void checkAllDevices();
}
```

##### 3) 指令下发模块
```java
@Service
public class CommandService {
    // 修改 URL
    public void changeUrl(String deviceId, String newUrl);
    
    // 刷新页面
    public void refreshPage(String deviceId);
    
    // 重建 WebView
    public void rebuildWebView(String deviceId);
    
    // 更新配置
    public void updateConfig(String deviceId, DeviceConfig config);
    
    // 批量操作
    public void batchCommand(List<String> deviceIds, Command command);
}
```

---

## 三、数据库设计

### 1. 数据库表结构

#### 表1: tv_device (设备表)
```sql
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
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_last_heartbeat` (`last_heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TV设备表';
```

#### 表2: tv_device_config (设备配置表)
```sql
CREATE TABLE `tv_device_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID',
  `display_url` VARCHAR(512) NOT NULL COMMENT '展示的URL',
  `refresh_interval` INT NOT NULL DEFAULT 0 COMMENT '定时刷新间隔(秒), 0表示不刷新',
  `rebuild_interval` INT NOT NULL DEFAULT 0 COMMENT '定时重建WebView间隔(秒), 0表示不重建',
  `enable_kiosk_mode` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用Kiosk模式: 0-否, 1-是',
  `enable_white_screen_check` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用白屏检测: 0-否, 1-是',
  `heartbeat_interval` INT NOT NULL DEFAULT 30 COMMENT '心跳间隔(秒)',
  `ws_server_url` VARCHAR(256) NOT NULL COMMENT 'WebSocket服务器地址',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  CONSTRAINT `fk_config_device` FOREIGN KEY (`device_id`) REFERENCES `tv_device` (`device_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='TV设备配置表';
```

#### 表3: tv_command_log (指令日志表)
```sql
CREATE TABLE `tv_command_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `command_id` VARCHAR(64) NOT NULL COMMENT '指令ID(UUID)',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID',
  `command_type` VARCHAR(32) NOT NULL COMMENT '指令类型: CHANGE_URL, REFRESH, REBUILD_WEBVIEW, UPDATE_CONFIG, RESTART_APP',
  `command_content` TEXT DEFAULT NULL COMMENT '指令内容(JSON)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '执行状态: 0-待执行, 1-已发送, 2-执行成功, 3-执行失败',
  `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
  `ack_time` DATETIME DEFAULT NULL COMMENT '设备确认时间',
  `result_message` VARCHAR(512) DEFAULT NULL COMMENT '执行结果消息',
  `operator` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_command_id` (`command_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指令日志表';
```

#### 表4: tv_operation_log (操作日志表)
```sql
CREATE TABLE `tv_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator` VARCHAR(64) NOT NULL COMMENT '操作人',
  `operation_type` VARCHAR(32) NOT NULL COMMENT '操作类型: DEVICE_ADD, DEVICE_UPDATE, DEVICE_DELETE, COMMAND_SEND, CONFIG_UPDATE',
  `operation_content` TEXT DEFAULT NULL COMMENT '操作内容描述',
  `target_device_id` VARCHAR(64) DEFAULT NULL COMMENT '目标设备ID',
  `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operator` (`operator`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
```

#### 表5: tv_heartbeat_log (心跳日志表 - 可选)
```sql
CREATE TABLE `tv_heartbeat_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID',
  `heartbeat_time` DATETIME NOT NULL COMMENT '心跳时间',
  `cpu_usage` DECIMAL(5,2) DEFAULT NULL COMMENT 'CPU使用率(%)',
  `memory_usage` DECIMAL(5,2) DEFAULT NULL COMMENT '内存使用率(%)',
  `disk_usage` DECIMAL(5,2) DEFAULT NULL COMMENT '磁盘使用率(%)',
  `network_status` TINYINT DEFAULT NULL COMMENT '网络状态: 0-异常, 1-正常',
  `webview_status` TINYINT DEFAULT NULL COMMENT 'WebView状态: 0-异常, 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='心跳日志表(建议按月分表)';
```

### 2. 初始化 SQL 脚本

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS `tv_management` 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `tv_management`;

-- 执行上述建表语句...

-- 插入测试数据
INSERT INTO `tv_device` 
  (`device_id`, `device_name`, `device_model`, `android_version`, `app_version`, `location`, `status`, `register_time`)
VALUES
  ('TV001', '生产车间-1号屏', 'Xiaomi TV Box S', 'Android 11', '1.0.0', '1号车间-入口', 1, NOW()),
  ('TV002', '生产车间-2号屏', 'Xiaomi TV Box S', 'Android 11', '1.0.0', '2号车间-中央', 0, NOW());

INSERT INTO `tv_device_config`
  (`device_id`, `display_url`, `refresh_interval`, `rebuild_interval`, `ws_server_url`)
VALUES
  ('TV001', 'http://192.168.1.100/dashboard', 300, 3600, 'ws://192.168.1.200:8080/ws'),
  ('TV002', 'http://192.168.1.100/production', 600, 7200, 'ws://192.168.1.200:8080/ws');
```

---

## 四、通信协议设计

### 1. WebSocket 消息格式

#### 基础消息结构
```json
{
  "type": "MESSAGE_TYPE",
  "timestamp": 1678886400000,
  "data": {}
}
```

### 2. 消息类型定义

#### 2.1 设备注册 (TV -> Server)
```json
{
  "type": "REGISTER",
  "timestamp": 1678886400000,
  "data": {
    "deviceId": "TV001",
    "deviceName": "生产车间-1号屏",
    "deviceModel": "Xiaomi TV Box S",
    "androidVersion": "Android 11",
    "appVersion": "1.0.0",
    "ipAddress": "192.168.1.101",
    "macAddress": "00:11:22:33:44:55"
  }
}
```

#### 2.2 注册响应 (Server -> TV)
```json
{
  "type": "REGISTER_ACK",
  "timestamp": 1678886400000,
  "data": {
    "success": true,
    "message": "注册成功",
    "config": {
      "displayUrl": "http://192.168.1.100/dashboard",
      "refreshInterval": 300,
      "rebuildInterval": 3600,
      "heartbeatInterval": 30
    }
  }
}
```

#### 2.3 心跳上报 (TV -> Server)
```json
{
  "type": "HEARTBEAT",
  "timestamp": 1678886400000,
  "data": {
    "deviceId": "TV001",
    "status": "NORMAL",
    "cpuUsage": 35.5,
    "memoryUsage": 62.3,
    "webviewStatus": "RUNNING",
    "currentUrl": "http://192.168.1.100/dashboard"
  }
}
```

#### 2.4 心跳响应 (Server -> TV)
```json
{
  "type": "HEARTBEAT_ACK",
  "timestamp": 1678886400000,
  "data": {
    "received": true
  }
}
```

#### 2.5 修改 URL 指令 (Server -> TV)
```json
{
  "type": "CHANGE_URL",
  "timestamp": 1678886400000,
  "data": {
    "commandId": "CMD-UUID-001",
    "url": "http://192.168.1.100/new-dashboard"
  }
}
```

#### 2.6 刷新页面指令 (Server -> TV)
```json
{
  "type": "REFRESH",
  "timestamp": 1678886400000,
  "data": {
    "commandId": "CMD-UUID-002"
  }
}
```

#### 2.7 重建 WebView 指令 (Server -> TV)
```json
{
  "type": "REBUILD_WEBVIEW",
  "timestamp": 1678886400000,
  "data": {
    "commandId": "CMD-UUID-003"
  }
}
```

#### 2.8 更新配置指令 (Server -> TV)
```json
{
  "type": "UPDATE_CONFIG",
  "timestamp": 1678886400000,
  "data": {
    "commandId": "CMD-UUID-004",
    "config": {
      "refreshInterval": 600,
      "rebuildInterval": 7200,
      "heartbeatInterval": 60
    }
  }
}
```

#### 2.9 指令执行结果 (TV -> Server)
```json
{
  "type": "COMMAND_RESULT",
  "timestamp": 1678886400000,
  "data": {
    "commandId": "CMD-UUID-001",
    "success": true,
    "message": "URL修改成功"
  }
}
```

---

## 五、关键技术实现要点

### 1. TV 端关键实现

#### 1.1 WebView 防内存泄漏
```kotlin
fun destroyWebView() {
    webView?.apply {
        stopLoading()
        settings.javaScriptEnabled = false
        clearHistory()
        removeAllViews()
        destroy()
    }
    webView = null
}
```

#### 1.2 自动重连机制
```kotlin
private fun reconnect() {
    reconnectAttempts++
    val delay = min(30000L, 1000L * (2.0.pow(reconnectAttempts).toLong()))
    Handler(Looper.getMainLooper()).postDelayed({
        connect()
    }, delay)
}
```

#### 1.3 白屏检测
```kotlin
fun checkWhiteScreen(): Boolean {
    val bitmap = Bitmap.createBitmap(
        webView.width, webView.height, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    webView.draw(canvas)
    
    // 检测是否全白
    return isAllWhite(bitmap)
}
```

### 2. 服务端关键实现

#### 2.1 心跳超时检测
```java
@Scheduled(fixedRate = 30000)
public void checkHeartbeatTimeout() {
    long now = System.currentTimeMillis();
    heartbeatMap.forEach((deviceId, lastTime) -> {
        if (now - lastTime > 60000) { // 60秒超时
            deviceService.markOffline(deviceId);
            sessionManager.removeSession(deviceId);
        }
    });
}
```

#### 2.2 消息可靠性保证
```java
public void sendCommand(String deviceId, Command command) {
    // 1. 先入库
    commandLogService.save(command);
    
    // 2. 发送消息
    try {
        sessionManager.sendToDevice(deviceId, command.toJson());
        commandLogService.updateStatus(command.getId(), CommandStatus.SENT);
    } catch (Exception e) {
        commandLogService.updateStatus(command.getId(), CommandStatus.FAILED);
    }
}
```

---

## 六、部署方案

### 1. 服务端部署

#### 环境要求
- JDK 17
- MySQL 8.0
- 至少 2GB 内存
- 内网固定 IP

#### 部署步骤
```bash
# 1. 数据库初始化
mysql -u root -p < init.sql

# 2. 修改配置文件
vim application.yml

# 3. 打包
mvn clean package

# 4. 启动
java -jar tvservice.jar
```

### 2. TV 端部署

#### 安装步骤
1. 安装 APK
2. 首次启动输入服务器地址
3. 授予必要权限(存储、网络)
4. 设置为系统启动器(Kiosk 模式)
5. 开机自启

---

## 七、后续扩展建议

1. **安全性增强**
   - 设备认证 Token
   - WebSocket 使用 WSS (TLS)
   - 操作权限管理

2. **功能扩展**
   - 远程截屏
   - 实时日志查看
   - OTA 升级
   - 统计报表

3. **高可用**
   - 服务端集群部署
   - 数据库主从复制
   - Redis 缓存

4. **监控告警**
   - 设备离线告警
   - 异常日志告警
   - 性能监控

---

## 八、总结

该方案基于成熟的技术栈,架构清晰、可实现性强。通过 WebSocket 长连接实现实时控制,结合定时任务和异常恢复机制,能够满足企业内网环境下统一管理多台 Android TV 的需求。

**核心优势:**
- ✅ 技术栈成熟稳定
- ✅ 实时性强(WebSocket)
- ✅ 可扩展性好
- ✅ 运维成本低
- ✅ 完全内网部署

**建议开发顺序:**
1. 搭建服务端基础框架 + 数据库
2. 实现 WebSocket 连接管理
3. 开发 TV 端 WebView 展示
4. 实现心跳机制
5. 实现指令下发功能
6. 开发管理后台
7. 完善异常处理和监控
8. 测试与优化
