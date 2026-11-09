package cn.xny.tvservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 连接会话管理器
 * 负责维护 deviceId -> WebSocketSession 的映射
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionDeviceMap = new ConcurrentHashMap<>();

    /** deviceId -> base64 JPEG 截图（内存中，不持久化）*/
    private final ConcurrentHashMap<String, String> screenshotCache = new ConcurrentHashMap<>();

    public void saveScreenshot(String deviceId, String base64Jpeg) {
        screenshotCache.put(deviceId, base64Jpeg);
    }

    public String getScreenshot(String deviceId) {
        return screenshotCache.get(deviceId);
    }

    public void clearScreenshot(String deviceId) {
        screenshotCache.remove(deviceId);
    }

    public void register(String deviceId, WebSocketSession session) {
        // 如果已有旧连接，先关闭
        WebSocketSession old = sessions.get(deviceId);
        if (old != null && old.isOpen()) {
            try {
                old.close();
            } catch (IOException ignored) {}
        }
        sessions.put(deviceId, session);
        sessionDeviceMap.put(session.getId(), deviceId);
        log.info("设备连接注册: deviceId={}, sessionId={}", deviceId, session.getId());
    }

    public void remove(WebSocketSession session) {
        String deviceId = sessionDeviceMap.remove(session.getId());
        if (deviceId != null) {
            sessions.remove(deviceId);
            log.info("设备连接移除: deviceId={}, sessionId={}", deviceId, session.getId());
        }
    }

    public void removeByDeviceId(String deviceId) {
        WebSocketSession session = sessions.remove(deviceId);
        if (session != null) {
            sessionDeviceMap.remove(session.getId());
        }
    }

    public String getDeviceIdBySession(WebSocketSession session) {
        return sessionDeviceMap.get(session.getId());
    }

    public boolean isOnline(String deviceId) {
        WebSocketSession session = sessions.get(deviceId);
        return session != null && session.isOpen();
    }

    public void sendToDevice(String deviceId, String message) {
        WebSocketSession session = sessions.get(deviceId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("发送消息失败 -> 设备:{}, 错误:{}", deviceId, e.getMessage());
                throw new RuntimeException("发送消息失败: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("设备不在线: " + deviceId);
        }
    }

    public void broadcast(String message) {
        sessions.forEach((deviceId, session) -> {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    log.error("广播消息失败 -> 设备:{}", deviceId);
                }
            }
        });
    }

    public List<String> getOnlineDeviceIds() {
        List<String> online = new ArrayList<>();
        sessions.forEach((deviceId, session) -> {
            if (session.isOpen()) online.add(deviceId);
        });
        return online;
    }

    public int getOnlineCount() {
        return (int) sessions.values().stream().filter(WebSocketSession::isOpen).count();
    }

    public Map<String, WebSocketSession> getAllSessions() {
        return sessions;
    }
}
