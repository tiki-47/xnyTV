package cn.xny.tvservice.service;

import cn.xny.tvservice.entity.HeartbeatLog;
import cn.xny.tvservice.mapper.HeartbeatLogMapper;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HeartbeatLogService {

    private final HeartbeatLogMapper heartbeatLogMapper;

    public void saveHeartbeat(String deviceId, JSONObject data) {
        HeartbeatLog log = new HeartbeatLog();
        log.setDeviceId(deviceId);
        log.setHeartbeatTime(LocalDateTime.now());

        if (data.containsKey("cpuUsage")) {
            log.setCpuUsage(data.getBigDecimal("cpuUsage"));
        }
        if (data.containsKey("memoryUsage")) {
            log.setMemoryUsage(data.getBigDecimal("memoryUsage"));
        }
        if (data.containsKey("diskUsage")) {
            log.setDiskUsage(data.getBigDecimal("diskUsage"));
        }
        if (data.containsKey("networkStatus")) {
            log.setNetworkStatus("NORMAL".equals(data.getString("networkStatus")) ? 1 : 0);
        }
        if (data.containsKey("webviewStatus")) {
            String ws = data.getString("webviewStatus");
            log.setWebviewStatus(switch (ws) {
                case "RUNNING" -> 1;
                case "REBUILDING" -> 2;
                default -> 0;
            });
        }
        if (data.containsKey("currentUrl")) {
            log.setCurrentUrl(data.getString("currentUrl"));
        }

        heartbeatLogMapper.insert(log);
    }

    public List<HeartbeatLog> listByDevice(String deviceId, int limit) {
        return heartbeatLogMapper.selectList(new LambdaQueryWrapper<HeartbeatLog>()
                .eq(HeartbeatLog::getDeviceId, deviceId)
                .orderByDesc(HeartbeatLog::getHeartbeatTime)
                .last("LIMIT " + limit));
    }
}
