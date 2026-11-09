# xnyTV — TV / Kiosk 小主机集中管理系统

> 由 TV 端、Kiosk 小主机端和服务端组成的企业内网大屏集中管理系统，支持网页展示、远程控制、实时监控与统一配置。

源码仓库：[GitHub](https://github.com/tiki-47/xnyTV)。项目使用 [MIT License](./LICENSE)。

**部署范围：** 当前版本面向受信任的内网，尚未实现完整的管理员登录、设备认证和权限控制。请勿将管理端口直接暴露到公网；公开源码不代表服务可直接用于公网部署。

## 📋 项目简介

本系统用于在企业内网环境中，统一管理 Android TV 和连接显示屏的 Windows 小主机，实现网页看板、宣传页、生产看板等内容的集中展示与控制。

系统包含三个端，可根据显示设备选择 TV 端或 Kiosk 小主机端，并通过服务端统一管理：

| 端 | 项目目录 | 运行环境 | 主要用途 |
|----|----------|----------|----------|
| TV 端 | `tvclient/` | Android TV、安卓电视盒子 | 通过 WebView 全屏展示网页，接收远程指令并上报心跳 |
| Kiosk 小主机端 | `xnykiosk/` | 连接电视或显示器的 Windows 小主机 / PC | 通过 Electron 全屏展示网页，支持本地配置和服务端集中管理 |
| 服务端 | `tvservice/` | JDK 17 + MySQL 8.0 | 提供 Web 管理后台、设备管理、状态监控和远程指令下发 |

## ✨ 核心功能

### TV 端功能
- ✅ 全屏 WebView 展示指定 URL 网页
- ✅ 定时刷新网页(可配置间隔)
- ✅ 定时重建 WebView(防卡死、防内存泄漏)
- ✅ 接收服务端实时控制指令
- ✅ 心跳上报(实时监控)
- ✅ 异常自动恢复(白屏/崩溃检测)
- ✅ WebSocket 自动重连
- ✅ 开机自启动
- ✅ Kiosk 模式(限制用户退出)

### Kiosk 小主机端功能
- ✅ 全屏锁定展示指定网页，窗口置顶
- ✅ 定时刷新网页与崩溃、无响应自动恢复
- ✅ 开机自启动
- ✅ 本地配置展示网址、缩放比例与服务端地址
- ✅ WebSocket 连接、断线重连与心跳上报
- ✅ 接收服务端指令，远程修改网址、刷新页面与更新配置
- ✅ 可独立运行，也可接入服务端统一管理

### 服务端功能
- ✅ 设备注册与管理
- ✅ WebSocket 长连接管理
- ✅ 设备在线状态实时监控
- ✅ 远程下发控制指令
- ✅ 设备参数动态配置
- ✅ 心跳接收与离线判断
- ✅ 操作日志完整记录
- ✅ Web 管理后台

## 🏗️ 技术架构

### TV 端
- **平台**: Android TV
- **语言**: Kotlin
- **核心技术**: WebView + WebSocket + Handler
- **最低版本**: Android 5.0 (API 21)

### Kiosk 小主机端
- **平台**: Windows 小主机 / PC，外接电视或显示器
- **框架**: Electron
- **语言**: JavaScript + HTML + CSS
- **通信**: WebSocket
- **打包**: electron-builder，支持 Windows 安装包和便携版

### 服务端
- **框架**: Spring Boot 3.3.5
- **语言**: Java 17
- **通信**: WebSocket
- **数据库**: MySQL 8.0
- **ORM**: MyBatis-Plus

## 📁 项目结构

```
xnyTV/
├── tvclient/                 # Android TV 客户端
│   ├── app/
│   │   └── src/main/java/com/xny/tvclient/
│   │       ├── MainActivity.kt
│   │       ├── WebViewActivity.kt
│   │       ├── service/         # WebSocket、心跳、保活服务
│   │       ├── manager/         # WebView、配置、指令管理
│   │       └── receiver/        # 开机自启广播
│   └── build.gradle.kts
│
├── tvservice/                # Spring Boot 服务端
│   ├── src/main/java/cn/xny/tvservice/
│   │   ├── controller/      # REST API 控制器
│   │   ├── service/         # 业务逻辑层
│   │   ├── entity/          # 数据库实体
│   │   ├── mapper/          # MyBatis Mapper
│   │   ├── websocket/       # WebSocket 处理器
│   │   └── config/          # 配置类
│   └── pom.xml
│
├── database/                # 数据库脚本
│   └── init.sql            # 初始化脚本(包含表结构+测试数据)
│
├── xnykiosk/                # Kiosk 小主机端（Electron / Windows）
├── deploy/                  # Windows 构建、部署与打包脚本
├── LICENSE                  # MIT 开源许可证
├── DESIGN.md               # 完整设计文档(必读)
└── README.md               # 本文件
```

## 🚀 快速开始

### 1. 环境准备

#### 服务端环境
- JDK 17+
- MySQL 8.0+
- Maven 3.6+

#### TV 端环境
- Android TV 设备(API 21+)
- 网络连接(内网)

#### Kiosk 小主机端环境
- Windows 小主机或 PC，连接电视或显示器
- 源码运行和打包需要 Node.js、npm
- 接入集中管理时，需与服务端网络互通

### 2. 数据库初始化

`database/init.sql` 包含演示设备与示例账号数据，并含有 `DROP TABLE` 语句。请仅在新建的演示数据库运行；已有数据库应先备份并按需迁移。脚本中的地址与账号不应直接用于生产环境。

```bash
# 连接 MySQL
mysql -u root -p

# 执行初始化脚本
source database/init.sql

# 或直接导入
mysql -u root -p < database/init.sql
```

### 3. 服务端启动

数据库密码通过环境变量提供，不写入源码。PowerShell 示例（变量仅对当前终端及其子进程生效）：

```powershell
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = Read-Host "MySQL 密码" -MaskInput
# 数据库不在本机时，可额外设置完整 JDBC URL：
# $env:SPRING_DATASOURCE_URL = "jdbc:mysql://数据库主机:3306/tv_management"
```

Windows PowerShell 5.1 不支持 `-MaskInput`，可使用 PowerShell 7 或在本机通过系统环境变量设置密码。服务模式需要为运行服务的账号配置相同变量。Spring Boot 不会自动加载 `.env` 文件。

```bash
cd tvservice

# 提前在当前终端配置数据库环境变量

# 编译打包
mvn clean package

# 启动服务
java -jar target/tvservice-0.0.1-SNAPSHOT.jar

# 或使用 Maven 直接运行
mvn spring-boot:run
```

访问服务端: http://localhost:8080

### 4. TV 端安装

构建需要 JDK 17、Android SDK 35，并在 Android Studio 或 `JAVA_HOME` 中配置 JDK。`tvclient/local.properties` 为本机 SDK 设置，不提交到仓库。

```bash
cd tvclient

# 构建 APK
./gradlew assembleDebug

# 生成的 APK 路径
# app/build/outputs/apk/debug/app-debug.apk
```

安装步骤:
1. 将 APK 复制到 TV 设备
2. 安装应用
3. 首次启动配置服务器地址(WebSocket URL)
4. 授予必要权限
5. (可选)设置为系统启动器实现 Kiosk 模式

### 5. Kiosk 小主机端（Windows）

安装 Node.js 和 npm 后，在 PowerShell 执行：

```powershell
cd xnykiosk
npm ci
Copy-Item config.example.json config.json
# 编辑 config.json，设置展示网址及可选的 WebSocket 服务地址
npm start
```

`config.json` 是本地配置，已排除提交。应用启动后将配置保存到 Electron 的用户数据目录。详细操作见 [PC 客户端说明](./xnykiosk/README.md)。

## 📊 数据库设计

### 核心表结构

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| tv_device | 设备表 | device_id, device_name, status, last_heartbeat_time |
| tv_device_config | 设备配置表 | display_url, refresh_interval, rebuild_interval |
| tv_command_log | 指令日志表 | command_type, status, send_time, ack_time |
| tv_operation_log | 操作日志表 | operator, operation_type, create_time |
| tv_heartbeat_log | 心跳日志表 | device_id, heartbeat_time, cpu_usage, memory_usage |
| tv_user | 管理员表 | username, password, role |

详细设计见: [数据库设计文档](./database/init.sql)

## 🔌 通信协议

### WebSocket 消息格式

基础结构:
```json
{
  "type": "MESSAGE_TYPE",
  "timestamp": 1678886400000,
  "data": {}
}
```

### 主要消息类型

#### 1. 设备注册 (TV → Server)
```json
{
  "type": "REGISTER",
  "data": {
    "deviceId": "TV001",
    "deviceName": "生产车间-1号屏",
    "androidVersion": "Android 11",
    "appVersion": "1.0.0"
  }
}
```

#### 2. 心跳上报 (TV → Server)
```json
{
  "type": "HEARTBEAT",
  "data": {
    "deviceId": "TV001",
    "status": "NORMAL",
    "cpuUsage": 35.5,
    "memoryUsage": 62.3
  }
}
```

#### 3. 修改 URL (Server → TV)
```json
{
  "type": "CHANGE_URL",
  "data": {
    "commandId": "CMD-UUID-001",
    "url": "http://192.168.1.100/new-dashboard"
  }
}
```

#### 4. 刷新页面 (Server → TV)
```json
{
  "type": "REFRESH",
  "data": {
    "commandId": "CMD-UUID-002"
  }
}
```

更多协议详情见: [DESIGN.md](./DESIGN.md)

## 🎯 核心功能实现

### WebView 防内存泄漏
- 定时重建 WebView
- 及时释放资源
- 销毁时完整清理

### 自动重连机制
- 断线自动重连
- 指数退避算法
- 最大重连次数限制

### 白屏检测
- 定时检测 WebView 渲染状态
- 检测到白屏自动重建
- 防止页面卡死

### 心跳超时判断
- 设备端定时上报心跳(30秒)
- 服务端定时检测超时(60秒)
- 超时自动标记离线

## 🔒 安全性说明

当前版本为企业内网部署版本，安全性考虑:

- ✅ 运行在企业内网(物理隔离)
- ✅ 操作日志完整记录
- ⚠️ WebSocket 未加密(可升级为 WSS)
- ⚠️ 设备认证较简单(可增加 Token)
- ⚠️ 后台权限管理简单(可细化)

**建议生产环境增强**:
1. 使用 WSS (WebSocket over TLS)
2. 添加设备认证 Token
3. 实现细粒度权限控制
4. 添加操作审计

## 📈 性能优化

### TV 端
- WebView 硬件加速
- 图片缓存策略
- 定时内存回收
- 进程保活机制

### 服务端
- WebSocket 连接池管理
- 心跳数据定期清理
- 数据库索引优化
- Redis 缓存(可选)

## 🔧 配置说明

### TV 端配置项 (SharedPreferences)
```kotlin
// 服务器地址
ws_server_url: String = "ws://192.168.1.200:8080/ws"

// 展示 URL
display_url: String = "http://192.168.1.100/dashboard"

// 刷新间隔(秒, 0=不刷新)
refresh_interval: Int = 300

// 重建间隔(秒, 0=不重建)
rebuild_interval: Int = 3600

// 心跳间隔(秒)
heartbeat_interval: Int = 30
```

### 服务端配置 (application.yml)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tv_management
    username: root
    password: ${SPRING_DATASOURCE_PASSWORD}

websocket:
  path: /ws
  heartbeat-timeout: 60000  # 心跳超时时间(毫秒)
```

## 🐛 故障排查

### TV 端常见问题

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 无法连接服务器 | 网络不通/地址错误 | 检查网络、确认服务器地址 |
| 频繁重启 | 内存不足 | 增加重建间隔、优化网页 |
| 白屏 | 页面加载失败 | 检查 URL、查看网页日志 |
| 无法退出应用 | Kiosk 模式 | 长按电源键重启设备 |

### 服务端常见问题

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 设备显示离线 | 心跳超时 | 检查 TV 网络、查看心跳日志 |
| 指令发送失败 | WebSocket 断开 | 检查连接状态、查看日志 |
| 数据库连接失败 | 配置错误 | 检查数据库配置 |

## 📝 TODO List

已有服务端、Android 客户端、Electron 客户端及 Web 管理界面的基础实现，后续计划：

- [ ] 添加用户认证与授权
- [ ] 实现远程截屏功能
- [ ] 实现实时日志查看
- [ ] 添加统计报表功能
- [ ] 实现 OTA 升级功能
- [ ] 添加告警通知(邮件/短信)
- [ ] 编写单元测试
- [ ] 编写接口文档
- [ ] 性能压测与优化

## 📚 参考文档

- [完整设计方案](./DESIGN.md) - **必读!** 包含详细的架构设计、可行性分析等
- [数据库初始化脚本](./database/init.sql) - 数据库表结构及初始化数据
- [Spring Boot WebSocket 官方文档](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Android WebView 官方文档](https://developer.android.com/reference/android/webkit/WebView)

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request!

## 📄 License

[MIT License](./LICENSE)

## 👨‍💻 作者

[2764134654@qq.com](mailto:2764134654@qq.com)

---

## ⚡ 快速命令参考

```bash
# 数据库初始化
mysql -u root -p < database/init.sql

# 服务端启动
cd tvservice && mvn spring-boot:run

# TV 端构建
cd tvclient && ./gradlew assembleDebug

# 清理心跳日志(MySQL)
CALL sp_clean_heartbeat_log();

# 查看在线设备
SELECT * FROM v_device_status_overview WHERE status = 1;

# 查看最近指令
SELECT * FROM v_recent_commands LIMIT 20;
```

---

**开始使用前，请务必阅读 [DESIGN.md](./DESIGN.md) 了解完整的系统设计!**
