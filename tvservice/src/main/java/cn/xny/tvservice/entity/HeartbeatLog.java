package cn.xny.tvservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tv_heartbeat_log")
public class HeartbeatLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceId;
    private LocalDateTime heartbeatTime;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private BigDecimal diskUsage;

    /**
     * 网络状态: 0-异常, 1-正常
     */
    private Integer networkStatus;

    /**
     * WebView状态: 0-异常, 1-正常, 2-重建中
     */
    private Integer webviewStatus;

    private String currentUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
