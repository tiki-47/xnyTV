package cn.xny.tvservice.service;

import cn.xny.tvservice.dto.DeviceStatusDTO;
import cn.xny.tvservice.entity.Device;
import cn.xny.tvservice.entity.DeviceConfig;

import java.util.List;

public interface DeviceService {

    /**
     * 设备注册或更新信息
     */
    Device registerOrUpdate(Device device);

    /**
     * 标记设备在线
     */
    void markOnline(String deviceId);

    /**
     * 标记设备离线
     */
    void markOffline(String deviceId);

    /**
     * 标记设备异常
     */
    void markException(String deviceId);

    /**
     * 获取所有设备状态列表
     */
    List<DeviceStatusDTO> listDeviceStatus();

    /**
     * 根据deviceId获取设备
     */
    Device getByDeviceId(String deviceId);

    /**
     * 获取设备配置
     */
    DeviceConfig getConfig(String deviceId);

    /**
     * 更新设备配置
     */
    void updateConfig(String deviceId, DeviceConfig config);

    /**
     * 删除设备
     */
    void deleteDevice(String deviceId);

    /**
     * 更新最后心跳时间
     */
    void updateHeartbeat(String deviceId);

    /**
     * 获取所有超时离线的设备ID（超过60s未心跳）
     */
    List<String> getTimeoutDeviceIds(long timeoutMs);
}
