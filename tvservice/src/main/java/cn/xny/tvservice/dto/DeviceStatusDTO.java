package cn.xny.tvservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备状态视图 DTO
 */
@Data
public class DeviceStatusDTO {

    private String deviceId;
    private String deviceName;
    private String location;
    private Integer status;
    private String statusName;
    private LocalDateTime lastHeartbeatTime;
    private Long offlineSeconds;
    private String displayUrl;
    private Integer refreshInterval;
    private Integer rebuildInterval;

    // 设备硬件信息
    private String ipAddress;
    private String macAddress;
    private String deviceModel;
    private String androidVersion;
    private String appVersion;
    private LocalDateTime registerTime;

    // 心跳带上来的实时信息
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private String currentUrl;
    private Integer webviewStatus;

    public String getStatusName() {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "离线";
            case 1 -> "在线";
            case 2 -> "异常";
            default -> "未知";
        };
    }
}
