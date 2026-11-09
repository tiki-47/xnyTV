package cn.xny.tvservice.controller;

import cn.xny.tvservice.common.R;
import cn.xny.tvservice.dto.SendCommandDTO;
import cn.xny.tvservice.entity.CommandLog;
import cn.xny.tvservice.service.CommandService;
import cn.xny.tvservice.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 指令下发接口
 */
@RestController
@RequestMapping("/api/command")
@RequiredArgsConstructor
public class CommandController {

    private final CommandService commandService;
    private final OperationLogService operationLogService;

    /**
     * 下发指令（支持批量）
     */
    @PostMapping("/send")
    public R<Void> send(@RequestBody SendCommandDTO dto,
                        @RequestHeader(value = "X-Operator", defaultValue = "admin") String operator) {
        dto.setOperator(operator);
        commandService.sendCommand(dto);
        operationLogService.save(operator, "COMMAND_SEND",
                "下发指令: type=" + dto.getCommandType() + ", devices=" + dto.getDeviceIds(),
                dto.getDeviceIds().size() == 1 ? dto.getDeviceIds().get(0) : null, null);
        return R.ok();
    }

    /**
     * 查询最近指令记录
     */
    @GetMapping("/recent")
    public R<List<CommandLog>> recent(@RequestParam(defaultValue = "50") int limit) {
        return R.ok(commandService.listRecent(limit));
    }

    /**
     * 查询指定设备的指令历史
     */
    @GetMapping("/device/{deviceId}")
    public R<List<CommandLog>> byDevice(@PathVariable String deviceId,
                                         @RequestParam(defaultValue = "20") int limit) {
        return R.ok(commandService.listByDeviceId(deviceId, limit));
    }
}
