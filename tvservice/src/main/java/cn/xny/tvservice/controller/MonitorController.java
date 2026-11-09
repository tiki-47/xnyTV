package cn.xny.tvservice.controller;

import cn.xny.tvservice.common.R;
import cn.xny.tvservice.common.WsMessage;
import cn.xny.tvservice.entity.OperationLog;
import cn.xny.tvservice.service.OperationLogService;
import cn.xny.tvservice.websocket.WebSocketSessionManager;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 监控相关接口（含截图）
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final WebSocketSessionManager sessionManager;
    private final OperationLogService operationLogService;

    @GetMapping("/connections")
    public R<Map<String, Object>> connections() {
        return R.ok(Map.of(
                "onlineCount", sessionManager.getOnlineCount(),
                "onlineDevices", sessionManager.getOnlineDeviceIds()
        ));
    }

    @GetMapping("/operation-log")
    public R<List<OperationLog>> operationLog(@RequestParam(defaultValue = "50") int limit) {
        return R.ok(operationLogService.listRecent(limit));
    }

    @GetMapping("/operation-log/{deviceId}")
    public R<List<OperationLog>> deviceOperationLog(@PathVariable String deviceId,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return R.ok(operationLogService.listByDevice(deviceId, limit));
    }

    /**
     * 向指定设备发送截图请求
     * POST /api/monitor/{deviceId}/screenshot/request
     */
    @PostMapping("/{deviceId}/screenshot/request")
    public R<Void> requestScreenshot(@PathVariable String deviceId) {
        if (!sessionManager.isOnline(deviceId)) {
            return R.fail("设备不在线");
        }
        sessionManager.clearScreenshot(deviceId);
        String screenshotId = deviceId + "_" + System.currentTimeMillis();
        JSONObject data = new JSONObject();
        data.put("screenshotId", screenshotId);
        WsMessage msg = new WsMessage(WsMessage.TYPE_SCREENSHOT_REQ, data);
        sessionManager.sendToDevice(deviceId, msg.toJson());
        return R.ok();
    }

    /**
     * 获取设备最新截图（base64 JPEG）
     * GET /api/monitor/{deviceId}/screenshot
     */
    @GetMapping("/{deviceId}/screenshot")
    public R<Map<String, String>> getScreenshot(@PathVariable String deviceId) {
        String base64 = sessionManager.getScreenshot(deviceId);
        if (base64 == null) {
            return R.fail("暂无截图，请先发送截图请求");
        }
        return R.ok(Map.of("deviceId", deviceId, "imageBase64", base64));
    }
}
