package cn.xny.tvservice.service.impl;

import cn.xny.tvservice.dto.DeviceStatusDTO;
import cn.xny.tvservice.entity.Device;
import cn.xny.tvservice.entity.DeviceConfig;
import cn.xny.tvservice.mapper.DeviceConfigMapper;
import cn.xny.tvservice.mapper.DeviceMapper;
import cn.xny.tvservice.service.DeviceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final DeviceConfigMapper deviceConfigMapper;

    @Override
    @Transactional
    public Device registerOrUpdate(Device device) {
        Device existing = getByDeviceId(device.getDeviceId());
        if (existing == null) {
            device.setStatus(1);
            device.setRegisterTime(LocalDateTime.now());
            device.setLastHeartbeatTime(LocalDateTime.now());
            deviceMapper.insert(device);
            log.info("新设备注册: {}", device.getDeviceId());
        } else {
            existing.setDeviceName(device.getDeviceName() != null ? device.getDeviceName() : existing.getDeviceName());
            existing.setDeviceModel(device.getDeviceModel() != null ? device.getDeviceModel() : existing.getDeviceModel());
            existing.setAndroidVersion(device.getAndroidVersion() != null ? device.getAndroidVersion() : existing.getAndroidVersion());
            existing.setAppVersion(device.getAppVersion() != null ? device.getAppVersion() : existing.getAppVersion());
            existing.setIpAddress(device.getIpAddress() != null ? device.getIpAddress() : existing.getIpAddress());
            existing.setMacAddress(device.getMacAddress() != null ? device.getMacAddress() : existing.getMacAddress());
            existing.setStatus(1);
            existing.setLastHeartbeatTime(LocalDateTime.now());
            deviceMapper.updateById(existing);
            device = existing;
            log.info("设备重新上线: {}", device.getDeviceId());
        }
        return device;
    }

    @Override
    public void markOnline(String deviceId) {
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getDeviceId, deviceId)
                .set(Device::getStatus, 1)
                .set(Device::getLastHeartbeatTime, LocalDateTime.now()));
    }

    @Override
    public void markOffline(String deviceId) {
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getDeviceId, deviceId)
                .set(Device::getStatus, 0));
        log.info("设备离线: {}", deviceId);
    }

    @Override
    public void markException(String deviceId) {
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getDeviceId, deviceId)
                .set(Device::getStatus, 2));
    }

    @Override
    public List<DeviceStatusDTO> listDeviceStatus() {
        List<Device> devices = deviceMapper.selectList(null);
        List<DeviceStatusDTO> result = new ArrayList<>();
        for (Device d : devices) {
            DeviceStatusDTO dto = new DeviceStatusDTO();
            dto.setDeviceId(d.getDeviceId());
            dto.setDeviceName(d.getDeviceName());
            dto.setLocation(d.getLocation());
            dto.setStatus(d.getStatus());
            dto.setLastHeartbeatTime(d.getLastHeartbeatTime());
            dto.setIpAddress(d.getIpAddress());
            dto.setMacAddress(d.getMacAddress());
            dto.setDeviceModel(d.getDeviceModel());
            dto.setAndroidVersion(d.getAndroidVersion());
            dto.setAppVersion(d.getAppVersion());
            dto.setRegisterTime(d.getRegisterTime());
            if (d.getLastHeartbeatTime() != null) {
                long offline = (System.currentTimeMillis() -
                        d.getLastHeartbeatTime().toInstant(ZoneOffset.of("+8")).toEpochMilli()) / 1000;
                dto.setOfflineSeconds(offline);
            }
            DeviceConfig config = getConfig(d.getDeviceId());
            if (config != null) {
                dto.setDisplayUrl(config.getDisplayUrl());
                dto.setRefreshInterval(config.getRefreshInterval());
                dto.setRebuildInterval(config.getRebuildInterval());
            }
            result.add(dto);
        }
        return result;
    }

    @Override
    public Device getByDeviceId(String deviceId) {
        return deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceId, deviceId));
    }

    @Override
    public DeviceConfig getConfig(String deviceId) {
        return deviceConfigMapper.selectOne(new LambdaQueryWrapper<DeviceConfig>()
                .eq(DeviceConfig::getDeviceId, deviceId));
    }

    @Override
    @Transactional
    public void updateConfig(String deviceId, DeviceConfig config) {
        DeviceConfig existing = getConfig(deviceId);
        config.setDeviceId(deviceId);
        if (existing == null) {
            deviceConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            deviceConfigMapper.updateById(config);
        }
    }

    @Override
    @Transactional
    public void deleteDevice(String deviceId) {
        deviceMapper.delete(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceId, deviceId));
    }

    @Override
    public void updateHeartbeat(String deviceId) {
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getDeviceId, deviceId)
                .set(Device::getLastHeartbeatTime, LocalDateTime.now())
                .set(Device::getStatus, 1));
    }

    @Override
    public List<String> getTimeoutDeviceIds(long timeoutMs) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(timeoutMs / 1000);
        List<Device> timeoutDevices = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getStatus, 1)
                .lt(Device::getLastHeartbeatTime, threshold));
        return timeoutDevices.stream().map(Device::getDeviceId).toList();
    }
}
