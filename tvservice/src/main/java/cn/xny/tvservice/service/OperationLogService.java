package cn.xny.tvservice.service;

import cn.xny.tvservice.entity.OperationLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.xny.tvservice.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public void save(String operator, String operationType, String content,
                     String targetDeviceId, String ip) {
        OperationLog log = new OperationLog();
        log.setOperator(operator != null ? operator : "system");
        log.setOperationType(operationType);
        log.setOperationContent(content);
        log.setTargetDeviceId(targetDeviceId);
        log.setIpAddress(ip);
        operationLogMapper.insert(log);
    }

    public List<OperationLog> listRecent(int limit) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreateTime)
                .last("LIMIT " + limit));
    }

    public List<OperationLog> listByDevice(String deviceId, int limit) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getTargetDeviceId, deviceId)
                .orderByDesc(OperationLog::getCreateTime)
                .last("LIMIT " + limit));
    }
}
