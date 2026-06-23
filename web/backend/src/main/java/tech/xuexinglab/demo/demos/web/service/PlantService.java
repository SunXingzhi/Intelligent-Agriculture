package tech.xuexinglab.demo.demos.web.service;

import org.springframework.stereotype.Service;
import tech.xuexinglab.demo.demos.web.entity.PlantData;
import tech.xuexinglab.demo.demos.web.mapper.PlantDataMapper;
import tech.xuexinglab.demo.demos.web.websocket.SensorWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class PlantService {

        private final PlantDataMapper plantDataMapper;
        private final SensorWebSocketHandler webSocketHandler;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public PlantService(PlantDataMapper plantDataMapper, SensorWebSocketHandler webSocketHandler) {
                this.plantDataMapper = plantDataMapper;
                this.webSocketHandler = webSocketHandler;
        }

        public PlantData addData(PlantData data) {
                if (data.getRecordTime() == null) {
                        data.setRecordTime(LocalDateTime.now());
                }
                plantDataMapper.insert(data);
                // 通过 WebSocket 推送到前端
                try {
                        String json = objectMapper.writeValueAsString(data);
                        // 显式非空断言，消除 null safety 警告
                        webSocketHandler.broadcast(Objects.requireNonNull(json, "json must not be null"));
                } catch (Exception e) {
                        // 建议至少记录日志
                        // log.error("WebSocket推送失败", e);
                }
                return data;
        }

        public List<PlantData> getAllData() {
                return plantDataMapper.findAll();
        }

}