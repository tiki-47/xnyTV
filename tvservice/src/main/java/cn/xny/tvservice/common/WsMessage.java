package cn.xny.tvservice.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

/**
 * WebSocket 消息统一格式
 */
@Data
public class WsMessage {

    private String type;
    private Long timestamp;
    private Object data;

    public WsMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public WsMessage(String type, Object data) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.data = data;
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }

    public static WsMessage fromJson(String json) {
        return JSON.parseObject(json, WsMessage.class);
    }

    public JSONObject getDataAsJson() {
        if (data == null) return new JSONObject();
        if (data instanceof JSONObject) return (JSONObject) data;
        return JSON.parseObject(JSON.toJSONString(data));
    }

    // 常用消息类型常量
    public static final String TYPE_REGISTER = "REGISTER";
    public static final String TYPE_REGISTER_ACK = "REGISTER_ACK";
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";
    public static final String TYPE_HEARTBEAT_ACK = "HEARTBEAT_ACK";
    public static final String TYPE_CHANGE_URL = "CHANGE_URL";
    public static final String TYPE_REFRESH = "REFRESH";
    public static final String TYPE_REBUILD_WEBVIEW = "REBUILD_WEBVIEW";
    public static final String TYPE_UPDATE_CONFIG = "UPDATE_CONFIG";
    public static final String TYPE_RESTART_APP = "RESTART_APP";
    public static final String TYPE_COMMAND_RESULT = "COMMAND_RESULT";
    public static final String TYPE_PONG = "PONG";
    public static final String TYPE_SCREENSHOT_REQ = "SCREENSHOT_REQ";
    public static final String TYPE_SCREENSHOT_RESULT = "SCREENSHOT_RESULT";
}
