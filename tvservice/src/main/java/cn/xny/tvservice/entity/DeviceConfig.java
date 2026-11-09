package cn.xny.tvservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tv_device_config")
public class DeviceConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;

    /**
     * 展示的URL
     */
    private String displayUrl;

    /**
     * 定时刷新间隔(秒), 0表示不刷新
     */
    private Integer refreshInterval;

    /**
     * 定时重建WebView间隔(秒), 0表示不重建
     */
    private Integer rebuildInterval;

    /**
     * 是否启用Kiosk模式: 0-否, 1-是
     */
    private Integer enableKioskMode;

    /**
     * 是否启用白屏检测: 0-否, 1-是
     */
    private Integer enableWhiteScreenCheck;

    /**
     * 白屏检测间隔(秒)
     */
    private Integer whiteScreenCheckInterval;

    /**
     * 心跳间隔(秒)
     */
    private Integer heartbeatInterval;

    /**
     * WebSocket服务器地址
     */
    private String wsServerUrl;

    /**
     * 是否启用异常自动重启: 0-否, 1-是
     */
    private Integer enableAutoRestart;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
