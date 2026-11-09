package cn.xny.tvservice.controller;

import cn.xny.tvservice.common.R;
import cn.xny.tvservice.dto.DeviceStatusDTO;
import cn.xny.tvservice.entity.Device;
import cn.xny.tvservice.entity.DeviceConfig;
import cn.xny.tvservice.entity.HeartbeatLog;
import cn.xny.tvservice.service.DeviceService;
import cn.xny.tvservice.service.HeartbeatLogService;
import cn.xny.tvservice.service.OperationLogService;
import cn.xny.tvservice.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备管理接口
 */
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final WebSocketSessionManager sessionManager;
    private final HeartbeatLogService heartbeatLogService;
    private final OperationLogService operationLogService;

    /**
     * 获取所有设备状态列表
     */
    @GetMapping("/list")
    public R<List<DeviceStatusDTO>> list() {
        return R.ok(deviceService.listDeviceStatus());
    }

    /**
     * 获取单个设备详情
     */
    @GetMapping("/{deviceId}")
    public R<Device> detail(@PathVariable String deviceId) {
        Device device = deviceService.getByDeviceId(deviceId);
        if (device == null) return R.fail("设备不存在");
        return R.ok(device);
    }

    /**
     * 获取设备配置
     */
    @GetMapping("/{deviceId}/config")
    public R<DeviceConfig> getConfig(@PathVariable String deviceId) {
        return R.ok(deviceService.getConfig(deviceId));
    }

    /**
     * 更新设备配置
     */
    @PutMapping("/{deviceId}/config")
    public R<Void> updateConfig(@PathVariable String deviceId,
                                @RequestBody DeviceConfig config,
                                @RequestHeader(value = "X-Operator", defaultValue = "admin") String operator) {
        deviceService.updateConfig(deviceId, config);
        operationLogService.save(operator, "CONFIG_UPDATE",
                "更新设备配置: deviceId=" + deviceId, deviceId, null);
        return R.ok();
    }

    /**
     * 更新设备基本信息（名称、位置等）
     */
    @PutMapping("/{deviceId}")
    public R<Void> update(@PathVariable String deviceId,
                          @RequestBody Device device,
                          @RequestHeader(value = "X-Operator", defaultValue = "admin") String operator) {
        Device existing = deviceService.getByDeviceId(deviceId);
        if (existing == null) return R.fail("设备不存在");
        existing.setDeviceName(device.getDeviceName());
        existing.setLocation(device.getLocation());
        existing.setRemark(device.getRemark());
        deviceService.registerOrUpdate(existing);
        operationLogService.save(operator, "DEVICE_UPDATE",
                "更新设备信息: deviceId=" + deviceId, deviceId, null);
        return R.ok();
    }

    /**
     * 删除设备
     */
    @DeleteMapping("/{deviceId}")
    public R<Void> delete(@PathVariable String deviceId,
                          @RequestHeader(value = "X-Operator", defaultValue = "admin") String operator) {
        deviceService.deleteDevice(deviceId);
        sessionManager.removeByDeviceId(deviceId);
        operationLogService.save(operator, "DEVICE_DELETE",
                "删除设备: deviceId=" + deviceId, deviceId, null);
        return R.ok();
    }

    /**
     * 获取在线设备统计
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        List<DeviceStatusDTO> all = deviceService.listDeviceStatus();
        long online = all.stream().filter(d -> d.getStatus() == 1).count();
        long offline = all.stream().filter(d -> d.getStatus() == 0).count();
        long exception = all.stream().filter(d -> d.getStatus() == 2).count();
        return R.ok(Map.of(
                "total", all.size(),
                "online", online,
                "offline", offline,
                "exception", exception
        ));
    }

    /**
     * 获取设备心跳历史
     */
    @GetMapping("/{deviceId}/heartbeat")
    public R<List<HeartbeatLog>> heartbeatHistory(@PathVariable String deviceId,
                                                   @RequestParam(defaultValue = "20") int limit) {
        return R.ok(heartbeatLogService.listByDevice(deviceId, limit));
    }

    /**
     * 查询当前在线的 deviceId 列表（来自 WebSocket session）
     */
    @GetMapping("/online")
    public R<List<String>> onlineList() {
        return R.ok(sessionManager.getOnlineDeviceIds());
    }
}
