package cn.xny.tvservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tv_device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;
    private String deviceName;
    private String deviceModel;
    private String androidVersion;
    private String appVersion;
    private String ipAddress;
    private String macAddress;
    private String location;

    /**
     * 设备状态: 0-离线, 1-在线, 2-异常
     */
    private Integer status;

    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime registerTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
