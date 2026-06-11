package tech.xuexinglab.demo.demos.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private EnvironmentMapper environmentMapper;

    @Autowired
    private SensorWebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取所有环境数据
     */
    public List<User> getAllData() {
        return environmentMapper.findAll();
    }
    
    /**
     * 根据ID获取数据
     */
    public User getDataById(Long id) {
        return environmentMapper.findById(id);
    }
    
    /**
     * 获取最新数据
     */
    public User getLatestData() {
        return environmentMapper.findLatest();
    }
    
    /**
     * 添加环境数据，并通过 WebSocket 推送到前端
     */
    public User addData(User user) {
        // 设置记录时间
        if (user.getRecordTime() == null) {
            user.setRecordTime(LocalDateTime.now());
        }
        environmentMapper.insert(user);
        // 数据入库后推送到前端
        broadcastData(user);
        return user;
    }

    /**
     * 通过 WebSocket 广播数据
     */
    private void broadcastData(User user) {
        try {
            String json = objectMapper.writeValueAsString(user);
            webSocketHandler.broadcast(json);
            log.info("WebSocket 推送成功: {}", json);
        } catch (Exception e) {
            log.error("WebSocket 推送失败", e);
        }
    }
    
    /**
     * 更新环境数据
     */
    public User updateData(Long id, User user) {
        user.setId(id);
        environmentMapper.update(user);
        return environmentMapper.findById(id);
    }
    
    /**
     * 删除环境数据
     */
    public boolean deleteData(Long id) {
        return environmentMapper.deleteById(id) > 0;
    }
    
    /**
     * 根据温度范围查询
     */
    public List<User> findByTemperatureRange(double min, double max) {
        return environmentMapper.findByTemperatureRange(min, max);
    }
    
    /**
     * 获取统计数据
     */
    public Map<String, Object> getStatistics() {
        return environmentMapper.getStatistics();
    }
    
    /**
     * 批量添加数据，并通过 WebSocket 推送
     */
    public List<User> batchAddData(List<User> dataList) {
        for (User data : dataList) {
            if (data.getRecordTime() == null) {
                data.setRecordTime(LocalDateTime.now());
            }
            environmentMapper.insert(data);
            broadcastData(data);
        }
        return dataList;
    }
}