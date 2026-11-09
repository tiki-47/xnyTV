package cn.xny.tvservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tv_operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operator;

    /**
     * 操作类型: DEVICE_ADD, DEVICE_UPDATE, DEVICE_DELETE, COMMAND_SEND, CONFIG_UPDATE
     */
    private String operationType;

    private String operationContent;
    private String targetDeviceId;
    private String ipAddress;
    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
