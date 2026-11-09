package cn.xny.tvservice.scheduled;

import cn.xny.tvservice.service.DeviceService;
import cn.xny.tvservice.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 心跳超时检测定时任务
 * 每30秒扫描一次，将超过60秒未心跳的在线设备标记为离线
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatCheckTask {

    private final DeviceService deviceService;
    private final WebSocketSessionManager sessionManager;

    @Value("${tv.websocket.heartbeat-timeout:60000}")
    private long heartbeatTimeout;

    @Scheduled(fixedRateString = "${tv.websocket.heartbeat-check-interval:30000}")
    public void checkHeartbeatTimeout() {
        List<String> timeoutDeviceIds = deviceService.getTimeoutDeviceIds(heartbeatTimeout);
        if (timeoutDeviceIds.isEmpty()) return;

        log.info("发现 {} 台设备心跳超时，标记为离线: {}", timeoutDeviceIds.size(), timeoutDeviceIds);

        for (String deviceId : timeoutDeviceIds) {
            deviceService.markOffline(deviceId);
            // 清理 WebSocket session（可能已经断了，但session还在map中）
            if (!sessionManager.isOnline(deviceId)) {
                sessionManager.removeByDeviceId(deviceId);
            }
        }
    }
}
