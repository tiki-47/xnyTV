const { app, BrowserWindow, ipcMain, screen, globalShortcut, Menu, net, session } = require('electron');
const path = require('path');
const fs = require('fs');
const os = require('os');
const { exec } = require('child_process');
const AutoLaunch = require('auto-launch');
const WebSocket = require('ws');

// ===================== 配置管理 =====================
const userDataPath = app.getPath('userData');
const configPath = path.join(userDataPath, 'config.json');
const defaultConfigPath = path.join(__dirname, 'config.json');

let config = {
  url: 'https://example.com',
  reloadInterval: 900000,
  enableAutoReload: true,
  enableCrashRecovery: true,
  enableAutoLaunch: true,
  fullscreen: true,
  alwaysOnTop: true,
  zoomFactor: 1.0,
  forceViewport: true,
  wsServerUrl: '',        // 服务端 WebSocket 地址，如 ws://192.168.1.100:8080/ws
  deviceName: os.hostname(),  // 设备名称
  heartbeatInterval: 30   // 心跳间隔（秒）
};

function loadConfig() {
  try {
    if (fs.existsSync(configPath)) {
      const data = fs.readFileSync(configPath, 'utf8');
      config = { ...config, ...JSON.parse(data) };
    } else if (fs.existsSync(defaultConfigPath)) {
      const data = fs.readFileSync(defaultConfigPath, 'utf8');
      config = { ...config, ...JSON.parse(data) };
      saveConfig();
    }
  } catch (error) {
    console.error('读取配置文件失败:', error);
  }
}

function saveConfig() {
  try {
    if (!fs.existsSync(userDataPath)) fs.mkdirSync(userDataPath, { recursive: true });
    fs.writeFileSync(configPath, JSON.stringify(config, null, 2));
  } catch (error) {
    console.error('保存配置文件失败:', error);
  }
}

// ===================== 设备ID =====================
function getDeviceId() {
  // 使用 MAC 地址 + hostname 生成稳定的 deviceId
  if (config.deviceId) return config.deviceId;
  const interfaces = os.networkInterfaces();
  let mac = '';
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (!iface.internal && iface.mac && iface.mac !== '00:00:00:00:00:00') {
        mac = iface.mac;
        break;
      }
    }
    if (mac) break;
  }
  const id = 'PC_' + (mac || os.hostname()).replace(/[^a-zA-Z0-9]/g, '').substring(0, 12).toUpperCase();
  config.deviceId = id;
  saveConfig();
  return id;
}

function getMacAddress() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (!iface.internal && iface.mac && iface.mac !== '00:00:00:00:00:00') {
        return iface.mac;
      }
    }
  }
  return 'unknown';
}

function getIpAddress() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (!iface.internal && iface.family === 'IPv4') {
        return iface.address;
      }
    }
  }
  return '127.0.0.1';
}

// ===================== 缓存管理 =====================
// Electron 的 Chromium 缓存与系统 Chrome 完全隔离，存储在 userData 目录。
// 若看板页面的 JS 资源被旧版本缓存（含硬编码的旧时间戳或旧初始化逻辑），
// 会导致页面显示的时间与浏览器不一致。启动时清空缓存确保每次拿到最新资源。
async function clearAppCache() {
  try {
    await session.defaultSession.clearCache();
    console.log('[Cache] HTTP 缓存已清除');
  } catch (e) {
    console.error('[Cache] 清除缓存失败:', e.message);
  }
}

// ===================== 自启动 =====================
const autoLauncher = new AutoLaunch({ name: 'XNY Kiosk', path: app.getPath('exe') });

// ===================== 窗口对象 =====================
let mainWindow = null;
let configWindow = null;
let reloadTimer = null;
let focusGuardInterval = null; // 焦点守护定时器

// ===================== WebSocket 客户端 =====================
let wsClient = null;
let wsConnected = false;
let wsReconnectTimer = null;
let wsHeartbeatTimer = null;

function initWebSocket() {
  if (!config.wsServerUrl) {
    console.log('[WS] 未配置服务器地址，跳过连接');
    return;
  }
  connectWebSocket();
}

function connectWebSocket() {
  if (wsClient) {
    try { wsClient.terminate(); } catch (_) {}
    wsClient = null;
  }

  console.log('[WS] 连接到:', config.wsServerUrl);
  try {
    wsClient = new WebSocket(config.wsServerUrl);

    wsClient.on('open', () => {
      console.log('[WS] 已连接');
      wsConnected = true;
      if (wsReconnectTimer) { clearTimeout(wsReconnectTimer); wsReconnectTimer = null; }
      sendWsRegister();
      startWsHeartbeat();
    });

    wsClient.on('message', (data) => {
      handleWsMessage(data.toString());
    });

    wsClient.on('close', () => {
      console.log('[WS] 连接断开，5秒后重连');
      wsConnected = false;
      stopWsHeartbeat();
      scheduleWsReconnect();
    });

    wsClient.on('error', (err) => {
      console.error('[WS] 连接错误:', err.message);
      wsConnected = false;
    });
  } catch (err) {
    console.error('[WS] 创建连接失败:', err.message);
    scheduleWsReconnect();
  }
}

function scheduleWsReconnect() {
  if (wsReconnectTimer) return;
  wsReconnectTimer = setTimeout(() => {
    wsReconnectTimer = null;
    if (config.wsServerUrl) connectWebSocket();
  }, 5000);
}

function sendWsMessage(msg) {
  if (wsClient && wsConnected && wsClient.readyState === WebSocket.OPEN) {
    wsClient.send(JSON.stringify(msg));
  }
}

function sendWsRegister() {
  sendWsMessage({
    type: 'REGISTER',
    timestamp: Date.now(),
    data: {
      deviceId: getDeviceId(),
      deviceName: config.deviceName || os.hostname(),
      deviceModel: 'Windows PC (' + process.arch + ')',
      androidVersion: os.platform() + ' ' + os.release(),
      appVersion: app.getVersion() || '1.0.0',
      macAddress: getMacAddress(),
      ipAddress: getIpAddress()
    }
  });
}

function startWsHeartbeat() {
  stopWsHeartbeat();
  const interval = (config.heartbeatInterval || 30) * 1000;
  wsHeartbeatTimer = setInterval(() => {
    sendWsMessage({
      type: 'HEARTBEAT',
      timestamp: Date.now(),
      data: {
        deviceId: getDeviceId(),
        status: 'NORMAL',
        currentUrl: config.url,
        webviewStatus: 'RUNNING'
      }
    });
  }, interval);
}

function stopWsHeartbeat() {
  if (wsHeartbeatTimer) { clearInterval(wsHeartbeatTimer); wsHeartbeatTimer = null; }
}

function sendWsCommandResult(commandId, success, message) {
  sendWsMessage({
    type: 'COMMAND_RESULT',
    timestamp: Date.now(),
    data: { commandId, success, message, deviceId: getDeviceId() }
  });
}

function handleWsMessage(text) {
  try {
    const msg = JSON.parse(text);
    const type = msg.type;
    const data = msg.data || {};
    console.log('[WS] 收到消息:', type);

    switch (type) {
      case 'REGISTER_ACK':
        console.log('[WS] 注册成功');
        // 如果服务端下发了配置（displayUrl），自动应用
        if (data.config && data.config.displayUrl && data.config.displayUrl !== 'about:blank') {
          if (mainWindow && config.url !== data.config.displayUrl) {
            config.url = data.config.displayUrl;
            saveConfig();
            mainWindow.loadURL(config.url);
          }
        }
        break;

      case 'HEARTBEAT_ACK':
        break;

      case 'REFRESH':
        // 刷新：忽略缓存重载当前页面（等同于 Ctrl+Shift+R）
        if (mainWindow) {
          mainWindow.webContents.reloadIgnoringCache();
          sendWsCommandResult(data.commandId, true, 'Refreshed');
          console.log('[Cmd] REFRESH -> reloadIgnoringCache');
        }
        break;

      case 'CHANGE_URL':
        if (data.url && mainWindow) {
          config.url = data.url;
          saveConfig();
          mainWindow.loadURL(data.url);
          sendWsCommandResult(data.commandId, true, 'URL changed to ' + data.url);
          console.log('[Cmd] CHANGE_URL ->', data.url);
        }
        break;

      case 'UPDATE_CONFIG': {
        const newCfg = data.config || data;
        if (newCfg.displayUrl && newCfg.displayUrl !== 'about:blank') {
          config.url = newCfg.displayUrl;
          if (mainWindow) mainWindow.loadURL(config.url);
        }
        if (newCfg.refreshInterval !== undefined) {
          config.reloadInterval = newCfg.refreshInterval * 1000;
        }
        saveConfig();
        applyConfigToWindow();
        sendWsCommandResult(data.commandId, true, 'Config updated');
        console.log('[Cmd] UPDATE_CONFIG ->', JSON.stringify(newCfg));
        break;
      }

      case 'REBUILD_WEBVIEW':
        // 硬刷新：清缓存重载（相当于 Ctrl+Shift+R）
        if (mainWindow) {
          mainWindow.webContents.reloadIgnoringCache();
          sendWsCommandResult(data.commandId, true, 'Hard reload done (PC)');
          console.log('[Cmd] REBUILD_WEBVIEW -> reloadIgnoringCache');
        }
        break;

      case 'RESTART_APP':
        app.relaunch();
        setTimeout(() => app.exit(0), 500);
        break;

      case 'SCREENSHOT_REQ':
        captureAndSendScreenshot(data.screenshotId);
        break;

      default:
        console.log('[WS] 未知消息类型:', type);
    }
  } catch (e) {
    console.error('[WS] 处理消息失败:', e.message);
  }
}

async function captureAndSendScreenshot(screenshotId) {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  try {
    console.log('[Screenshot] 开始截图:', screenshotId);
    const image = await mainWindow.webContents.capturePage();
    const resized = image.resize({ width: 960, height: 540, quality: 'good' });
    const jpegBuffer = resized.toJPEG(50);
    const base64 = jpegBuffer.toString('base64');
    sendWsMessage({
      type: 'SCREENSHOT_RESULT',
      timestamp: Date.now(),
      data: { screenshotId, imageBase64: base64, deviceId: getDeviceId() }
    });
    console.log('[Screenshot] 截图已发送，大小:', base64.length);
  } catch (e) {
    console.error('[Screenshot] 截图失败:', e.message);
  }
}

// ===================== 主窗口 =====================
function createWindow() {
  const primaryDisplay = screen.getPrimaryDisplay();
  const { width, height } = primaryDisplay.bounds;

  mainWindow = new BrowserWindow({
    width, height,
    fullscreen: config.fullscreen,
    frame: false,
    kiosk: config.fullscreen,
    alwaysOnTop: config.alwaysOnTop,
    autoHideMenuBar: true,
    backgroundColor: '#000000',
    skipTaskbar: false,
    focusable: true,
    minimizable: false,
    closable: true,
    webPreferences: {
      // 关闭 nodeIntegration：防止 process/require 等 Node.js 全局变量泄露到看板页面，
      // 避免 moment.js 等时间库检测到 Node.js 环境后走错误的时间初始化分支导致时间冻结
      nodeIntegration: false,
      contextIsolation: true,
      webviewTag: false,
      zoomFactor: config.zoomFactor || 1.0
    }
  });

  // 去掉 UA 中的 "Electron/x.x.x" 字样，防止页面通过 navigator.userAgent 检测到 Electron
  // 并走不同的代码路径（部分监控系统/第三方库对 Electron 环境有特殊处理）
  const rawUA = mainWindow.webContents.getUserAgent();
  const cleanUA = rawUA.replace(/\s*Electron\/[\d.]+/g, '');
  mainWindow.webContents.setUserAgent(cleanUA);
  console.log('[UA] User-Agent 已设置为:', cleanUA);

  if (config.alwaysOnTop) {
    mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
  }
  mainWindow.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen: true });
  if (config.fullscreen) mainWindow.setFullScreenable(true);

  mainWindow.loadURL(config.url);

  if (config.zoomFactor && config.zoomFactor !== 1.0) {
    mainWindow.webContents.once('did-finish-load', () => {
      mainWindow.webContents.setZoomFactor(config.zoomFactor);
    });
  }

  mainWindow.webContents.on('before-input-event', (event, input) => {
    if (input.control && input.shift && input.key.toLowerCase() === 'q') {
      event.preventDefault(); app.quit(); return;
    }
    if (input.control && input.shift && input.key.toLowerCase() === 'c') {
      event.preventDefault(); openConfigWindow(); return;
    }
    if (input.key === 'F12') {
      event.preventDefault(); openConfigWindow(); return;
    }
    if (input.control && !input.shift) event.preventDefault();
    if (input.alt && input.key !== 'F12') event.preventDefault();
    if (input.meta) event.preventDefault();
    if (input.key === 'F11') event.preventDefault();
  });

  if (config.enableCrashRecovery) {
    mainWindow.webContents.on('render-process-gone', () => {
      setTimeout(() => { if (mainWindow && !mainWindow.isDestroyed()) mainWindow.reload(); }, 2000);
    });
    mainWindow.webContents.on('unresponsive', () => {
      setTimeout(() => { if (mainWindow && !mainWindow.isDestroyed()) mainWindow.reload(); }, 5000);
    });
    mainWindow.webContents.on('did-fail-load', () => {
      setTimeout(() => { if (mainWindow && !mainWindow.isDestroyed()) mainWindow.loadURL(config.url); }, 5000);
    });
  }

  if (config.enableAutoReload) startReloadTimer();

  mainWindow.on('closed', () => {
    mainWindow = null;
    stopFocusGuard();
    if (reloadTimer) { clearInterval(reloadTimer); reloadTimer = null; }
  });

  // 失去焦点时恢复，但仅在配置窗口未打开时
  mainWindow.on('blur', () => {
    setTimeout(() => {
      if (mainWindow && !mainWindow.isDestroyed() && !configWindow) {
        mainWindow.focus();
        mainWindow.moveTop();
      }
    }, 200);
  });

  mainWindow.on('minimize', (event) => {
    if (!configWindow) {
      event.preventDefault();
      mainWindow.restore();
      mainWindow.focus();
    }
  });

  mainWindow.on('hide', () => {
    if (!configWindow && mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.show();
      mainWindow.focus();
    }
  });

  mainWindow.webContents.on('context-menu', (event, params) => {
    event.preventDefault();
    const currentZoom = config.zoomFactor || 1.0;
    const contextMenu = Menu.buildFromTemplate([
      { label: '⚙️ 打开配置 (Ctrl+Shift+C)', click: () => openConfigWindow() },
      { type: 'separator' },
      { label: `🔍 当前缩放: ${(currentZoom * 100).toFixed(0)}%`, enabled: false },
      { label: '➕ 放大', click: () => adjustZoom(0.1) },
      { label: '➖ 缩小', click: () => adjustZoom(-0.1) },
      { label: '🔄 重置 100%', click: () => setZoom(1.0) },
      { type: 'separator' },
      { label: '🔄 刷新页面', click: () => mainWindow && mainWindow.loadURL(config.url) },
      { label: '📁 打开配置文件夹', click: () => openConfigFolder() },
      { type: 'separator' },
      { label: '❌ 退出 (Ctrl+Shift+Q)', click: () => app.quit() }
    ]);
    contextMenu.popup();
  });

  mainWindow.webContents.on('did-finish-load', () => {
    injectPageScript();
  });

  // 启动焦点守护（仅在无配置窗口时生效）
  startFocusGuard();
}

function injectPageScript() {
  if (!mainWindow || mainWindow.isDestroyed()) return;

  mainWindow.webContents.executeJavaScript(`
    (function() {
      var viewport = document.querySelector('meta[name="viewport"]');
      if (!viewport) { viewport = document.createElement('meta'); viewport.name = 'viewport'; document.head.appendChild(viewport); }
      viewport.content = 'width=device-width, initial-scale=1.0';
      var style = document.createElement('style');
      style.textContent = 'html,body{width:100vw!important;height:100vh!important;margin:0!important;padding:0!important;overflow:auto!important}';
      document.head.appendChild(style);
      document.addEventListener('contextmenu', function(e){e.stopPropagation();}, true);
    })();
  `).catch(() => {});
}

function startFocusGuard() {
  stopFocusGuard();
  focusGuardInterval = setInterval(() => {
    // 只在配置窗口未打开、主窗口存在时才守护焦点
    if (configWindow) return;
    if (!mainWindow || mainWindow.isDestroyed()) return;
    if (mainWindow.isMinimized()) { mainWindow.restore(); mainWindow.focus(); return; }
    if (!mainWindow.isVisible()) { mainWindow.show(); mainWindow.focus(); return; }
    if (!mainWindow.isFocused()) { mainWindow.focus(); mainWindow.moveTop(); }
  }, 2000); // 改为2秒检查一次，降低CPU占用
}

function stopFocusGuard() {
  if (focusGuardInterval) { clearInterval(focusGuardInterval); focusGuardInterval = null; }
}

function applyConfigToWindow() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  if (config.zoomFactor) mainWindow.webContents.setZoomFactor(config.zoomFactor);
  if (config.enableAutoReload) startReloadTimer();
  else { if (reloadTimer) { clearInterval(reloadTimer); reloadTimer = null; } }
}

// ===================== 定时重载 =====================
function startReloadTimer() {
  if (reloadTimer) clearInterval(reloadTimer);
  if (!config.reloadInterval || config.reloadInterval < 1000) return;
  reloadTimer = setInterval(() => {
    if (mainWindow && !mainWindow.isDestroyed() && !configWindow) {
      console.log('[Reload] 定时重载页面（忽略缓存）...');
      // 使用 reloadIgnoringCache 而非 loadURL，确保不使用 Electron 本地缓存，
      // 避免看板 JS 被旧缓存命中导致时间戳等信息显示不正确
      mainWindow.webContents.reloadIgnoringCache();
    }
  }, config.reloadInterval);
  console.log('[Reload] 定时重载已启动，间隔:', config.reloadInterval / 1000, '秒');
}

// ===================== 配置窗口 =====================
function openConfigWindow() {
  if (configWindow) { configWindow.focus(); configWindow.show(); return; }

  // 停止焦点守护，让配置窗口可以正常交互
  stopFocusGuard();

  const wasKiosk = mainWindow && !mainWindow.isDestroyed() && mainWindow.isKiosk();
  const wasFullscreen = mainWindow && !mainWindow.isDestroyed() && mainWindow.isFullScreen();

  if (mainWindow && !mainWindow.isDestroyed()) {
    if (wasKiosk) mainWindow.setKiosk(false);
    if (wasFullscreen) mainWindow.setFullScreen(false);
    mainWindow.setAlwaysOnTop(false); // 临时取消置顶，让配置窗口显示在前面
  }

  configWindow = new BrowserWindow({
    width: 680,
    height: 800,
    frame: true,
    resizable: true,
    alwaysOnTop: true,
    minimizable: false,
    maximizable: false,
    skipTaskbar: false,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  configWindow.setAlwaysOnTop(true, 'pop-up-menu');
  configWindow.loadFile(path.join(__dirname, 'config.html'));
  configWindow.setMenuBarVisibility(false);

  configWindow.once('ready-to-show', () => {
    configWindow.show();
    configWindow.focus();
  });

  configWindow.on('closed', () => {
    configWindow = null;
    // 恢复主窗口状态
    if (mainWindow && !mainWindow.isDestroyed()) {
      if (wasKiosk && config.fullscreen) setTimeout(() => mainWindow && mainWindow.setKiosk(true), 200);
      else if (wasFullscreen && config.fullscreen) setTimeout(() => mainWindow && mainWindow.setFullScreen(true), 200);
      if (config.alwaysOnTop) setTimeout(() => {
        if (mainWindow && !mainWindow.isDestroyed()) {
          mainWindow.setAlwaysOnTop(true, 'screen-saver', 1);
          mainWindow.focus();
        }
      }, 300);
    }
    // 重新启动焦点守护
    startFocusGuard();
  });
}

function openConfigFolder() {
  const { shell } = require('electron');
  shell.showItemInFolder(configPath);
}

// ===================== 缩放控制 =====================
function adjustZoom(delta) {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  const currentZoom = config.zoomFactor || 1.0;
  setZoom(Math.max(0.5, Math.min(3.0, currentZoom + delta)));
}

function setZoom(zoom) {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  config.zoomFactor = Math.round(zoom * 100) / 100;
  mainWindow.webContents.setZoomFactor(config.zoomFactor);
  saveConfig();
  console.log('[Zoom] 缩放设置为:', (config.zoomFactor * 100).toFixed(0) + '%');
  mainWindow.webContents.executeJavaScript(`
    (function(){
      var o=document.getElementById('_zt');if(o)o.remove();
      o=document.createElement('div');o.id='_zt';
      o.style='position:fixed;top:20px;right:20px;background:rgba(0,0,0,0.75);color:#fff;padding:12px 22px;border-radius:8px;font-size:20px;font-weight:bold;z-index:999999;pointer-events:none';
      o.textContent='缩放: ${(config.zoomFactor * 100).toFixed(0)}%';
      document.body.appendChild(o);
      setTimeout(()=>o.remove(),2000);
    })();
  `).catch(() => {});
}

// ===================== 自启动 =====================
async function setupAutoLaunch() {
  try {
    const isEnabled = await autoLauncher.isEnabled();
    if (config.enableAutoLaunch && !isEnabled) await autoLauncher.enable();
    else if (!config.enableAutoLaunch && isEnabled) await autoLauncher.disable();
  } catch (error) {
    console.error('设置开机自启动失败:', error);
  }
}

// ===================== 全局快捷键 =====================
function registerGlobalShortcuts() {
  globalShortcut.register('CommandOrControl+Shift+C', () => openConfigWindow());
  globalShortcut.register('CommandOrControl+Shift+Q', () => app.quit());
  globalShortcut.register('CommandOrControl+=', () => adjustZoom(0.1));
  globalShortcut.register('CommandOrControl+-', () => adjustZoom(-0.1));
  globalShortcut.register('CommandOrControl+0', () => setZoom(1.0));
  console.log('[Shortcuts] 全局快捷键已注册');
}

// ===================== IPC 通信 =====================
ipcMain.on('get-config', (event) => {
  event.reply('config-data', config);
});

ipcMain.on('get-config-path', (event) => {
  event.reply('config-path', configPath);
});

// 立即应用配置，无需重启
ipcMain.on('update-config', (event, newConfig) => {
  const oldUrl = config.url;
  const oldReloadInterval = config.reloadInterval;
  const oldWsUrl = config.wsServerUrl;

  config = { ...config, ...newConfig };
  saveConfig();
  console.log('[Config] 配置已保存:', JSON.stringify(newConfig, null, 2));

  if (mainWindow && !mainWindow.isDestroyed()) {
    // 应用 URL 变更
    if (newConfig.url && newConfig.url !== oldUrl) {
      mainWindow.loadURL(config.url);
    }
    // 应用缩放
    if (newConfig.zoomFactor !== undefined) {
      mainWindow.webContents.setZoomFactor(config.zoomFactor);
    }
    // 应用全屏
    if (newConfig.fullscreen !== undefined) {
      mainWindow.setFullScreen(newConfig.fullscreen);
      mainWindow.setKiosk(newConfig.fullscreen);
    }
    // 应用置顶
    if (newConfig.alwaysOnTop !== undefined) {
      mainWindow.setAlwaysOnTop(newConfig.alwaysOnTop, 'screen-saver', 1);
    }
    // 重启定时器（如果间隔变了）
    if (newConfig.reloadInterval !== undefined && newConfig.reloadInterval !== oldReloadInterval) {
      if (config.enableAutoReload) startReloadTimer();
      else { if (reloadTimer) { clearInterval(reloadTimer); reloadTimer = null; } }
    } else if (newConfig.enableAutoReload !== undefined) {
      if (config.enableAutoReload) startReloadTimer();
      else { if (reloadTimer) { clearInterval(reloadTimer); reloadTimer = null; } }
    }
  }

  // 如果 WS 地址变了，重新连接
  if (newConfig.wsServerUrl !== undefined && newConfig.wsServerUrl !== oldWsUrl) {
    if (wsClient) { try { wsClient.terminate(); } catch (_) {} wsClient = null; wsConnected = false; }
    stopWsHeartbeat();
    if (config.wsServerUrl) {
      setTimeout(connectWebSocket, 1000);
    }
  }

  // 应用自启动
  setupAutoLaunch();

  event.reply('config-saved', { success: true });
});

ipcMain.on('open-config-folder', () => openConfigFolder());

// ===================== App 生命周期 =====================
app.whenReady().then(async () => {
  loadConfig();
  // 清除 Electron Chromium 缓存，确保看板页面加载最新资源，避免旧缓存导致时间显示错误
  await clearAppCache();
  registerGlobalShortcuts();
  createWindow();
  setupAutoLaunch();
  // 延迟初始化 WebSocket，等主窗口完全加载
  setTimeout(initWebSocket, 2000);

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
    else if (mainWindow && !mainWindow.isDestroyed()) { mainWindow.show(); mainWindow.focus(); }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    // Kiosk 模式下，窗口意外关闭时重新创建
    setTimeout(() => {
      if (BrowserWindow.getAllWindows().length === 0) createWindow();
    }, 1000);
  }
});

const gotTheLock = app.requestSingleInstanceLock();
if (!gotTheLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) { if (mainWindow.isMinimized()) mainWindow.restore(); mainWindow.focus(); }
  });
}

app.on('will-quit', () => {
  globalShortcut.unregisterAll();
  stopWsHeartbeat();
  if (wsClient) { try { wsClient.terminate(); } catch (_) {} }
});

console.log('[XNY Kiosk] 应用启动');
