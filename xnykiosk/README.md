# XNY Kiosk - 企业大屏幕数据看板应用

## 功能特点

✅ **全屏锁定显示** - 以 Kiosk 模式全屏显示指定网页，无边框，无菜单栏

✅ **定时自动重启** - 可配置定时重启间隔（默认15分钟），自动刷新页面

✅ **崩溃自动恢复** - 监控页面崩溃、无响应、加载失败等情况，自动重启页面

✅ **开机自启动** - 支持 Windows 开机自动启动

✅ **防止被顶掉** - 窗口始终置顶，失去焦点时自动重新获取焦点

✅ **安全退出** - 只能通过 `Ctrl+Shift+Q` 组合键退出应用

## 安装和运行

### 1. 安装依赖

```bash
npm install
```

### 2. 配置网址

先将 `config.example.json` 复制为 `config.json`，再修改要显示的网址和其他配置。本地 `config.json` 不提交到 Git。

```powershell
Copy-Item config.example.json config.json
```

```json
{
  "url": "https://your-dashboard-url.com",
  "reloadInterval": 900000,
  "enableAutoReload": true,
  "enableCrashRecovery": true,
  "enableAutoLaunch": true,
  "fullscreen": true,
  "alwaysOnTop": true
}
```

### 3. 运行应用

```bash
npm start
```

### 4. 打包应用（可选）

```bash
npm run build:win
```

打包后的安装程序位于 `dist` 目录下。

## 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `url` | 要显示的网页地址 | `https://www.example.com` |
| `reloadInterval` | 定时重启间隔（毫秒） | `900000` (15分钟) |
| `enableAutoReload` | 是否启用定时重启 | `true` |
| `enableCrashRecovery` | 是否启用崩溃恢复 | `true` |
| `enableAutoLaunch` | 是否启用开机自启动 | `true` |
| `fullscreen` | 是否全屏显示 | `true` |
| `alwaysOnTop` | 是否始终置顶 | `true` |
| `zoomFactor` | 页面缩放比例 (0.5-3.0) | `1.0` |
| `forceViewport` | 强制设置视口大小 | `true` |

### 分辨率适配

针对不同尺寸屏幕的推荐配置：

**32寸 FHD (1920x1080)**
```json
{
  "zoomFactor": 1.0
}
```

**55寸 FHD (1920x1080)**
```json
{
  "zoomFactor": 1.2
}
```

**55寸 4K (3840x2160)**
```json
{
  "zoomFactor": 1.5
}
```

**💡 实时调整（推荐）：**
- 右键点击窗口 → 快速缩放 → 选择比例
- 或使用快捷键 Ctrl+加号/减号 实时调整
- 无需重启，立即生效，自动保存

详细说明请参考：
- [实时缩放使用指南.md](实时缩放使用指南.md) - 快速上手
- [分辨率适配说明.md](分辨率适配说明.md) - 技术详解

## 使用说明

### 快捷键

- **Ctrl+Shift+C** - 打开配置管理窗口
- **Ctrl+Shift+Q** - 退出应用
- **Ctrl+加号(=)** - 实时放大页面（立即生效，无需重启）
- **Ctrl+减号(-)** - 实时缩小页面（立即生效，无需重启）
- **Ctrl+0** - 重置缩放为100%

### 修改网址和配置（三种方法）

#### 方法一：使用配置管理窗口（最简单 ⭐推荐）

1. 在运行的应用中按 **Ctrl+Shift+C**
2. 在弹出的配置窗口中修改网址和其他设置
3. 点击"保存并重启"按钮
4. 应用会自动重启并应用新配置

#### 方法二：直接编辑配置文件

1. 按 **Ctrl+Shift+C** 打开配置窗口
2. 点击"打开配置文件所在文件夹"
3. 编辑 `config.json` 文件
4. 重启应用

配置文件位置：`C:\Users\你的用户名\AppData\Roaming\xny-kiosk\config.json`

#### 方法三：重新打包（适合批量部署时预设配置）

1. 退出应用
2. 编辑项目根目录的 `config.json` 文件
3. 重新运行 `npm run build:win`
4. 使用新的安装程序部署

### 时间配置参考

- 5分钟 = 300000 毫秒
- 10分钟 = 600000 毫秒
- 15分钟 = 900000 毫秒
- 30分钟 = 1800000 毫秒
- 1小时 = 3600000 毫秒

## 技术架构

- **Electron** - 跨平台桌面应用框架
- **auto-launch** - 开机自启动功能
- **Node.js** - 后端运行时

## 故障排除

### 问题：应用无法启动

- 检查 Node.js 是否已安装
- 检查依赖是否已安装（运行 `npm install`）
- 检查 `config.json` 文件格式是否正确

### 问题：页面无法加载
- 检查网址是否正确
- 检查网络连接是否正常
- 查看控制台错误信息

### 问题：开机自启动不工作
- 确保 `enableAutoLaunch` 设置为 `true`
- 以管理员权限运行应用一次
- 检查 Windows 任务管理器的启动项

### 问题：被其他窗口覆盖（如网络认证页面）
- 应用已实施多层防护，自动恢复焦点
- 如果仍有问题，参考 [防止被其他窗口覆盖说明.md](防止被其他窗口覆盖说明.md)
- 联系IT部门将URL加入白名单
- 考虑使用Windows Kiosk模式

## 许可证

MIT License
