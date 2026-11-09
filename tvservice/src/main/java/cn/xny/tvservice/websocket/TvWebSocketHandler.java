package cn.xny.tvservice.websocket;

import cn.xny.tvservice.common.WsMessage;
import cn.xny.tvservice.entity.Device;
import cn.xny.tvservice.entity.DeviceConfig;
import cn.xny.tvservice.service.CommandService;
import cn.xny.tvservice.service.DeviceService;
import cn.xny.tvservice.service.HeartbeatLogService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * TV 设备 WebSocket 处理器
 * 处理设备的所有 WebSocket 消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TvWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final DeviceService deviceService;
    private final CommandService commandService;
    private final HeartbeatLogService heartbeatLogService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("新WebSocket连接建立: sessionId={}, remoteAddr={}",
                session.getId(), session.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("收到消息: sessionId={}, payload={}", session.getId(), payload);

        try {
            WsMessage wsMsg = WsMessage.fromJson(payload);
            if (wsMsg == null || wsMsg.getType() == null) {
                log.warn("收到格式错误的消息: {}", payload);
                return;
            }

            switch (wsMsg.getType()) {
                case WsMessage.TYPE_REGISTER -> handleRegister(session, wsMsg);
                case WsMessage.TYPE_HEARTBEAT -> handleHeartbeat(session, wsMsg);
                case WsMessage.TYPE_COMMAND_RESULT -> handleCommandResult(session, wsMsg);
                case WsMessage.TYPE_SCREENSHOT_RESULT -> handleScreenshotResult(session, wsMsg);
                default -> log.warn("未知消息类型: {}", wsMsg.getType());
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理设备注册
     */
    private void handleRegister(WebSocketSession session, WsMessage msg) {
        JSONObject data = msg.getDataAsJson();
        String deviceId = data.getString("deviceId");
        if (deviceId == null || deviceId.isBlank()) {
            log.warn("注册消息缺少deviceId");
            return;
        }

        // 注册或更新设备信息
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceName(data.getString("deviceName"));
        device.setDeviceModel(data.getString("deviceModel"));
        device.setAndroidVersion(data.getString("androidVersion"));
        device.setAppVersion(data.getString("appVersion"));
        device.setMacAddress(data.getString("macAddress"));

        // 获取真实IP
        String ip = data.getString("ipAddress");
        if (ip == null && session.getRemoteAddress() != null) {
            ip = session.getRemoteAddress().getAddress().getHostAddress();
        }
        device.setIpAddress(ip);

        deviceService.registerOrUpdate(device);

        // 绑定 session
        sessionManager.register(deviceId, session);

        // 查询设备配置，回包给设备
        DeviceConfig config = deviceService.getConfig(deviceId);

        JSONObject ackData = new JSONObject();
        ackData.put("success", true);
        ackData.put("message", "注册成功");

        JSONObject configJson = new JSONObject();
        if (config != null) {
            configJson.put("displayUrl", config.getDisplayUrl());
            configJson.put("refreshInterval", config.getRefreshInterval());
            configJson.put("rebuildInterval", config.getRebuildInterval());
            configJson.put("heartbeatInterval", config.getHeartbeatInterval());
            configJson.put("enableKioskMode", config.getEnableKioskMode());
            configJson.put("enableWhiteScreenCheck", config.getEnableWhiteScreenCheck());
        } else {
            // 默认配置
            configJson.put("displayUrl", "about:blank");
            configJson.put("refreshInterval", 0);
            configJson.put("rebuildInterval", 3600);
            configJson.put("heartbeatInterval", 30);
            configJson.put("enableKioskMode", 1);
            configJson.put("enableWhiteScreenCheck", 1);
        }
        ackData.put("config", configJson);

        WsMessage ack = new WsMessage(WsMessage.TYPE_REGISTER_ACK, ackData);
        sessionManager.sendToDevice(deviceId, ack.toJson());

        log.info("设备注册完成: deviceId={}, ip={}", deviceId, ip);
    }

    /**
     * 处理心跳
     */
    private void handleHeartbeat(WebSocketSession session, WsMessage msg) {
        JSONObject data = msg.getDataAsJson();
        String deviceId = data.getString("deviceId");

        if (deviceId == null) {
            // 尝试从 session 映射中获取
            deviceId = sessionManager.getDeviceIdBySession(session);
        }
        if (deviceId == null) {
            log.warn("心跳消息无deviceId");
            return;
        }

        // 更新数据库心跳时间
        deviceService.updateHeartbeat(deviceId);

        // 异步保存心跳日志
        heartbeatLogService.saveHeartbeat(deviceId, data);

        // 回包
        WsMessage ack = new WsMessage(WsMessage.TYPE_HEARTBEAT_ACK,
                JSONObject.of("received", true, "serverTime", System.currentTimeMillis()));
        try {
            sessionManager.sendToDevice(deviceId, ack.toJson());
        } catch (Exception ignored) {}

        log.debug("心跳收到: deviceId={}", deviceId);
    }

    /**
     * 处理指令执行结果
     */
    private void handleCommandResult(WebSocketSession session, WsMessage msg) {
        JSONObject data = msg.getDataAsJson();
        String commandId = data.getString("commandId");
        boolean success = Boolean.TRUE.equals(data.getBoolean("success"));
        String message = data.getString("message");

        commandService.updateCommandResult(commandId, success, message);

        log.info("指令执行结果: commandId={}, success={}, msg={}", commandId, success, message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
        onSessionClose(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
        onSessionClose(session);
    }

    /**
     * 处理截图回传
     */
    private void handleScreenshotResult(WebSocketSession session, WsMessage msg) {
        JSONObject data = msg.getDataAsJson();
        String deviceId = data.getString("deviceId");
        String base64 = data.getString("imageBase64");
        if (deviceId != null && base64 != null) {
            sessionManager.saveScreenshot(deviceId, base64);
            log.info("截图已缓存: deviceId={}, size={}", deviceId, base64.length());
        }
    }

    private void onSessionClose(WebSocketSession session) {
        String deviceId = sessionManager.getDeviceIdBySession(session);
        sessionManager.remove(session);
        if (deviceId != null) {
            deviceService.markOffline(deviceId);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
