package cn.xny.tvservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tv_command_log")
public class CommandLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String commandId;
    private String deviceId;

    /**
     * 指令类型: CHANGE_URL, REFRESH, REBUILD_WEBVIEW, UPDATE_CONFIG, RESTART_APP
     */
    private String commandType;

    /**
     * 指令内容(JSON)
     */
    private String commandContent;

    /**
     * 执行状态: 0-待执行, 1-已发送, 2-执行成功, 3-执行失败, 4-超时
     */
    private Integer status;

    private LocalDateTime sendTime;
    private LocalDateTime ackTime;
    private String resultMessage;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
