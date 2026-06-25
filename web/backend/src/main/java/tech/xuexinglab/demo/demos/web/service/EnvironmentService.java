package tech.xuexinglab.demo.demos.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.xuexinglab.demo.demos.web.entity.User;
import tech.xuexinglab.demo.demos.web.mapper.EnvironmentMapper;
import tech.xuexinglab.demo.demos.web.websocket.SensorWebSocketHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class EnvironmentService {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentService.class);

    private final EnvironmentMapper environmentMapper;
    private final SensorWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public EnvironmentService(EnvironmentMapper environmentMapper,
                              SensorWebSocketHandler webSocketHandler) {
        this.environmentMapper = environmentMapper;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = new ObjectMapper();
    }

    public List<User> getAllData() {
        return environmentMapper.findAll();
    }

    public User getDataById(Long id) {
        return environmentMapper.findById(id);
    }

    public User getLatestData() {
        return environmentMapper.findLatest();
    }

    public User addData(User user) {
        if (user.getRecordTime() == null) {
            user.setRecordTime(LocalDateTime.now());
        }
        validateAndCleanData(user);
        environmentMapper.insert(user);
        broadcastData(user);
        return user;
    }

    private void validateAndCleanData(User user) {
        // 二氧化碳浓度
        if (user.getCarbonConcentration() != null) {
            if (user.getCarbonConcentration() < 0 || user.getCarbonConcentration() > 10000) {
                user.setCarbonConcentration(null);
            }
        }
        // 温度
        if (user.getTemperature() != null) {
            if (user.getTemperature() < -50 || user.getTemperature() > 100) {
                user.setTemperature(null);
            }
        }
        // 空气湿度
        if (user.getAirHumidity() != null) {
            if (user.getAirHumidity() < 0 || user.getAirHumidity() > 100) {
                user.setAirHumidity(null);
            }
        }
        // 土壤湿度
        if (user.getSoilHumidity() != null) {
            if (user.getSoilHumidity() < 0 || user.getSoilHumidity() > 100) {
                user.setSoilHumidity(null);
            }
        }
        // 氮
        if (user.getNitrogen() != null) {
            if (user.getNitrogen() < 0 || user.getNitrogen() > 1000) {
                user.setNitrogen(null);
            }
        }
        // 磷
        if (user.getPhosphorus() != null) {
            if (user.getPhosphorus() < 0 || user.getPhosphorus() > 1000) {
                user.setPhosphorus(null);
            }
        }
        // 钾
        if (user.getPotassium() != null) {
            if (user.getPotassium() < 0 || user.getPotassium() > 1000) {
                user.setPotassium(null);
            }
        }
        // 光强
        if (user.getLightIntensity() != null) {
            if (user.getLightIntensity() < 0 || user.getLightIntensity() > 100000) {
                user.setLightIntensity(null);
            }
        }
        // ★ TVOC 替换 pH
        if (user.getTotalVolatileOrganicCompounds() != null) {
            if (user.getTotalVolatileOrganicCompounds() < 0 || user.getTotalVolatileOrganicCompounds() > 5000) {
                user.setTotalVolatileOrganicCompounds(null);
            }
        }
    }

    private void broadcastData(User user) {
        try {
            String json = objectMapper.writeValueAsString(user);
            if (json != null) {
                webSocketHandler.broadcast(json);
                log.info("WebSocket 推送成功: {}", json);
            } else {
                log.warn("生成的JSON为null，跳过推送");
            }
        } catch (Exception e) {
            log.error("WebSocket 推送失败", e);
        }
    }

    public User updateData(Long id, User user) {
        user.setId(id);
        environmentMapper.update(user);
        return environmentMapper.findById(id);
    }

    public boolean deleteData(Long id) {
        return environmentMapper.deleteById(id) > 0;
    }

    public List<User> findByTemperatureRange(double min, double max) {
        return environmentMapper.findByTemperatureRange(min, max);
    }

    public Map<String, Object> getStatistics() {
        return environmentMapper.getStatistics();
    }

    public List<User> batchAddData(List<User> dataList) {
        for (User data : dataList) {
            if (data.getRecordTime() == null) {
                data.setRecordTime(LocalDateTime.now());
            }
            validateAndCleanData(data);
            environmentMapper.insert(data);
            broadcastData(data);
        }
        return dataList;
    }
}