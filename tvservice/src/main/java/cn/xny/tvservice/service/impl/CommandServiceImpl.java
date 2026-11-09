package cn.xny.tvservice.service.impl;

import cn.xny.tvservice.common.WsMessage;
import cn.xny.tvservice.dto.SendCommandDTO;
import cn.xny.tvservice.entity.CommandLog;
import cn.xny.tvservice.mapper.CommandLogMapper;
import cn.xny.tvservice.service.CommandService;
import cn.xny.tvservice.websocket.WebSocketSessionManager;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandServiceImpl implements CommandService {

    private final CommandLogMapper commandLogMapper;
    private final WebSocketSessionManager sessionManager;

    @Override
    public void sendCommand(SendCommandDTO dto) {
        List<String> deviceIds = dto.getDeviceIds();
        if (deviceIds == null || deviceIds.isEmpty()) return;

        for (String deviceId : deviceIds) {
            String commandId = UUID.randomUUID().toString();

            // 构建指令内容
            JSONObject data = new JSONObject();
            data.put("commandId", commandId);

            String cmdType = dto.getCommandType();
            if (WsMessage.TYPE_CHANGE_URL.equals(cmdType)) {
                data.put("url", dto.getUrl());
            } else if (WsMessage.TYPE_UPDATE_CONFIG.equals(cmdType) && dto.getConfig() != null) {
                JSONObject configJson = new JSONObject();
                configJson.put("refreshInterval", dto.getConfig().getRefreshInterval());
                configJson.put("rebuildInterval", dto.getConfig().getRebuildInterval());
                configJson.put("heartbeatInterval", dto.getConfig().getHeartbeatInterval());
                configJson.put("enableKioskMode", dto.getConfig().getEnableKioskMode());
                data.put("config", configJson);
            }

            WsMessage msg = new WsMessage(dto.getCommandType(), data);

            // 入库
            CommandLog log = new CommandLog();
            log.setCommandId(commandId);
            log.setDeviceId(deviceId);
            log.setCommandType(dto.getCommandType());
            log.setCommandContent(data.toJSONString());
            log.setOperator(dto.getOperator());
            log.setStatus(0);
            commandLogMapper.insert(log);

            // 发送
            boolean online = sessionManager.isOnline(deviceId);
            if (online) {
                try {
                    sessionManager.sendToDevice(deviceId, msg.toJson());
                    commandLogMapper.update(null, new LambdaUpdateWrapper<CommandLog>()
                            .eq(CommandLog::getCommandId, commandId)
                            .set(CommandLog::getStatus, 1)
                            .set(CommandLog::getSendTime, LocalDateTime.now()));
                    log().info("指令已发送 -> 设备:{}, 类型:{}, 指令ID:{}", deviceId, dto.getCommandType(), commandId);
                } catch (Exception e) {
                    commandLogMapper.update(null, new LambdaUpdateWrapper<CommandLog>()
                            .eq(CommandLog::getCommandId, commandId)
                            .set(CommandLog::getStatus, 3)
                            .set(CommandLog::getResultMessage, e.getMessage()));
                    log().error("指令发送失败 -> 设备:{}, 错误:{}", deviceId, e.getMessage());
                }
            } else {
                commandLogMapper.update(null, new LambdaUpdateWrapper<CommandLog>()
                        .eq(CommandLog::getCommandId, commandId)
                        .set(CommandLog::getStatus, 3)
                        .set(CommandLog::getResultMessage, "设备不在线"));
                log().warn("设备不在线，指令无法发送 -> 设备:{}", deviceId);
            }
        }
    }

    @Override
    public void updateCommandResult(String commandId, boolean success, String message) {
        commandLogMapper.update(null, new LambdaUpdateWrapper<CommandLog>()
                .eq(CommandLog::getCommandId, commandId)
                .set(CommandLog::getStatus, success ? 2 : 3)
                .set(CommandLog::getResultMessage, message)
                .set(CommandLog::getAckTime, LocalDateTime.now()));
    }

    @Override
    public List<CommandLog> listByDeviceId(String deviceId, int limit) {
        return commandLogMapper.selectList(new LambdaQueryWrapper<CommandLog>()
                .eq(CommandLog::getDeviceId, deviceId)
                .orderByDesc(CommandLog::getCreateTime)
                .last("LIMIT " + limit));
    }

    @Override
    public List<CommandLog> listRecent(int limit) {
        return commandLogMapper.selectList(new LambdaQueryWrapper<CommandLog>()
                .orderByDesc(CommandLog::getCreateTime)
                .last("LIMIT " + limit));
    }

    private org.slf4j.Logger log() {
        return log;
    }
}
