package cn.xny.tvservice.service;

import cn.xny.tvservice.dto.SendCommandDTO;
import cn.xny.tvservice.entity.CommandLog;

import java.util.List;

public interface CommandService {

    /**
     * 向设备下发指令
     */
    void sendCommand(SendCommandDTO dto);

    /**
     * 更新指令执行结果
     */
    void updateCommandResult(String commandId, boolean success, String message);

    /**
     * 查询设备的指令历史
     */
    List<CommandLog> listByDeviceId(String deviceId, int limit);

    /**
     * 查询全部最近指令
     */
    List<CommandLog> listRecent(int limit);
}
