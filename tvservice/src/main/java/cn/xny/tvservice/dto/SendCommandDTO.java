package cn.xny.tvservice.dto;

import lombok.Data;

import java.util.List;

/**
 * 下发指令 DTO
 */
@Data
public class SendCommandDTO {

    /**
     * 设备ID列表(支持批量)
     */
    private List<String> deviceIds;

    /**
     * 指令类型: CHANGE_URL, REFRESH, REBUILD_WEBVIEW, UPDATE_CONFIG, RESTART_APP
     */
    private String commandType;

    /**
     * 新的展示URL (CHANGE_URL时必填)
     */
    private String url;

    /**
     * 配置参数 (UPDATE_CONFIG时使用)
     */
    private ConfigDTO config;

    /**
     * 操作人
     */
    private String operator;

    @Data
    public static class ConfigDTO {
        private Integer refreshInterval;
        private Integer rebuildInterval;
        private Integer heartbeatInterval;
        private Integer enableKioskMode;
        private Integer enableWhiteScreenCheck;
    }
}
