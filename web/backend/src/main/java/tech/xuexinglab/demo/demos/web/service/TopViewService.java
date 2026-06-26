package tech.xuexinglab.demo.demos.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.xuexinglab.demo.demos.web.entity.TopViewData;
import tech.xuexinglab.demo.demos.web.mapper.TopViewMapper;
import tech.xuexinglab.demo.demos.web.websocket.SensorWebSocketHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TopViewService {

    private static final Logger log = LoggerFactory.getLogger(TopViewService.class);
    private final TopViewMapper mapper;
    private final ObjectMapper objectMapper;
    private final SensorWebSocketHandler webSocketHandler;

    public TopViewService(TopViewMapper mapper, SensorWebSocketHandler webSocketHandler) {
        this.mapper = mapper;
        this.objectMapper = new ObjectMapper();
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 新增俯视图数据
     */
    public TopViewData addData(TopViewData data) {
        if (data.getDeviceAlias() == null || data.getImageIndex() == null) {
            throw new RuntimeException("设备别名和照片序号不能为空");
        }
        if (data.getRecordTime() == null) {
            data.setRecordTime(LocalDateTime.now());
        }

        mapper.insert(data);

        // WebSocket 推送（包含 imageData）
        broadcast(data);

        return data;
    }

    /**
     * 查询所有记录（不返回图片）
     */
    public List<TopViewData> getAll() {
        List<TopViewData> list = mapper.findAll();
        list.forEach(d -> d.setImageData(null));
        return list;
    }

    private void broadcast(TopViewData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            webSocketHandler.broadcast(Objects.requireNonNull(json, "json must not be null"));
            log.debug("Top view 推送成功: device={}, index={}", data.getDeviceAlias(), data.getImageIndex());
        } catch (Exception e) {
            log.error("Top view WebSocket 推送失败", e);
        }
    }
}