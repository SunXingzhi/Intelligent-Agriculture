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

                // 数据验证和清洗
                validateAndCleanData(user);

                // 保存到数据库
                environmentMapper.insert(user);

                // 通过WebSocket推送到前端
                broadcastData(user);

                return user;
        }

        /**
         * 验证并清洗数据
         */
        private void validateAndCleanData(User user) {
                // 验证二氧化碳浓度
                if (user.getCarbonConcentration() != null) {
                        if (user.getCarbonConcentration() < 0 || user.getCarbonConcentration() > 10000) {
                                user.setCarbonConcentration(null);
                        }
                }

                // 验证温度
                if (user.getTemperature() != null) {
                        if (user.getTemperature() < -50 || user.getTemperature() > 100) {
                                user.setTemperature(null);
                        }
                }

                // 验证空气湿度
                if (user.getAirHumidity() != null) {
                        if (user.getAirHumidity() < 0 || user.getAirHumidity() > 100) {
                                user.setAirHumidity(null);
                        }
                }

                // 验证土壤湿度
                if (user.getSoilHumidity() != null) {
                        if (user.getSoilHumidity() < 0 || user.getSoilHumidity() > 100) {
                                user.setSoilHumidity(null);
                        }
                }

                // 验证氮含量(N)
                if (user.getNitrogen() != null) {
                        if (user.getNitrogen() < 0 || user.getNitrogen() > 1000) {
                                user.setNitrogen(null);
                        }
                }

                // 验证磷含量(P)
                if (user.getPhosphorus() != null) {
                        if (user.getPhosphorus() < 0 || user.getPhosphorus() > 1000) {
                                user.setPhosphorus(null);
                        }
                }

                // 验证钾含量(K)
                if (user.getPotassium() != null) {
                        if (user.getPotassium() < 0 || user.getPotassium() > 1000) {
                                user.setPotassium(null);
                        }
                }

                // 验证光强
                if (user.getLightIntensity() != null) {
                        if (user.getLightIntensity() < 0 || user.getLightIntensity() > 100000) {
                                user.setLightIntensity(null);
                        }
                }

                // 验证pH值
                if (user.getPh() != null) {
                        if (user.getPh() < 0 || user.getPh() > 14) {
                                user.setPh(null);
                        }
                }
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

                        // 数据验证
                        validateAndCleanData(data);

                        // 保存到数据库
                        environmentMapper.insert(data);

                        // 通过WebSocket推送
                        broadcastData(data);
                }
                return dataList;
        }
}