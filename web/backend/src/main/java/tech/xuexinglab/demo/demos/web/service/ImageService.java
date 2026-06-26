package tech.xuexinglab.demo.demos.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.xuexinglab.demo.demos.web.entity.FrontViewData;
import tech.xuexinglab.demo.demos.web.entity.TopViewData;
import tech.xuexinglab.demo.demos.web.mapper.FrontViewMapper;
import tech.xuexinglab.demo.demos.web.mapper.TopViewMapper;
import tech.xuexinglab.demo.demos.web.websocket.SensorWebSocketHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    @Value("${app.image.upload-dir:uploads/images/}")
    private String uploadDir;

    private final SensorWebSocketHandler webSocketHandler;
    private final FrontViewMapper frontViewMapper;
    private final TopViewMapper topViewMapper;
    private final ObjectMapper objectMapper;

    public ImageService(SensorWebSocketHandler webSocketHandler,
                        FrontViewMapper frontViewMapper,
                        TopViewMapper topViewMapper) {
        this.webSocketHandler = webSocketHandler;
        this.frontViewMapper = frontViewMapper;
        this.topViewMapper = topViewMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 统一入口：根据 deviceAlias 前缀分流
     */
    public Map<String, Object> processDetection(Map<String, Object> requestData) {
        String deviceAlias = (String) requestData.get("deviceAlias");
        if (deviceAlias == null || deviceAlias.trim().isEmpty()) {
            throw new RuntimeException("deviceAlias 不能为空，且必须以 'front' 或 'top' 开头");
        }

        String lowerAlias = deviceAlias.toLowerCase();
        if (lowerAlias.startsWith("front")) {
            return processFrontView(requestData);
        } else if (lowerAlias.startsWith("top")) {
            return processTopView(requestData);
        } else {
            throw new RuntimeException("deviceAlias 必须以 'front' 或 'top' 开头，当前值: " + deviceAlias);
        }
    }

    // ---------- 前视图处理 ----------
    private Map<String, Object> processFrontView(Map<String, Object> data) {
        String deviceAlias = (String) data.get("deviceAlias");
        Integer imageIndex = (Integer) data.get("imageIndex");
        String imageData = (String) data.get("imageData");
        String timestamp = (String) data.get("timestamp");

        List<?> rawTomatoList = (List<?>) data.get("tomatoList");
        if (deviceAlias == null || imageIndex == null || imageData == null || rawTomatoList == null) {
            throw new RuntimeException("缺少必要字段: deviceAlias, imageIndex, imageData, tomatoList");
        }

        // 直接从 rawTomatoList 生成格式化字符串
        String formattedTomatoList = formatTomatoListFromRaw(rawTomatoList);
        int count = rawTomatoList.size();

        // 保存图片到本地
        byte[] imageBytes = decodeBase64Image(imageData);
        String savedImagePath = saveImageToLocal(imageBytes, deviceAlias, imageIndex, "front");

        // 构造记录
        FrontViewData record = new FrontViewData();
        record.setDeviceAlias(deviceAlias);
        record.setImageIndex(imageIndex);
        record.setRecordTime(parseTime(timestamp));
        record.setTomatoCount(count);
        record.setTomatoList(formattedTomatoList);   // 存入字符串
        record.setImageData(imageData);             // 仅用于推送

        frontViewMapper.insert(record);
        broadcastFrontView(record);

        // 响应
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "前视图数据接收成功，图片保存至 " + savedImagePath);
        resp.put("id", record.getId());
        resp.put("deviceAlias", deviceAlias);
        resp.put("imageIndex", imageIndex);
        resp.put("tomatoCount", count);
        resp.put("timestamp", record.getRecordTime().toString());
        resp.put("imagePath", savedImagePath);
        resp.put("tomatoList", formattedTomatoList);   // 返回格式化字符串
        return resp;
    }

    // ---------- 俯视图处理 ----------
    private Map<String, Object> processTopView(Map<String, Object> data) {
        String deviceAlias = (String) data.get("deviceAlias");
        Integer imageIndex = (Integer) data.get("imageIndex");
        String imageData = (String) data.get("imageData");
        String timestamp = (String) data.get("timestamp");
        Double stemDiameter = data.get("stemDiameter") != null ?
                ((Number) data.get("stemDiameter")).doubleValue() : null;

        if (deviceAlias == null || imageIndex == null || imageData == null || stemDiameter == null) {
            throw new RuntimeException("缺少必要字段: deviceAlias, imageIndex, imageData, stemDiameter");
        }

        byte[] imageBytes = decodeBase64Image(imageData);
        String savedImagePath = saveImageToLocal(imageBytes, deviceAlias, imageIndex, "top");

        TopViewData record = new TopViewData();
        record.setDeviceAlias(deviceAlias);
        record.setImageIndex(imageIndex);
        record.setRecordTime(parseTime(timestamp));
        record.setStemDiameter(stemDiameter);
        record.setImageData(imageData);

        topViewMapper.insert(record);
        broadcastTopView(record);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "俯视图数据接收成功，图片保存至 " + savedImagePath);
        resp.put("id", record.getId());
        resp.put("deviceAlias", deviceAlias);
        resp.put("imageIndex", imageIndex);
        resp.put("stemDiameter", stemDiameter);
        resp.put("timestamp", record.getRecordTime().toString());
        resp.put("imagePath", savedImagePath);
        return resp;
    }

    // ========== WebSocket 推送 ==========
    private void broadcastFrontView(FrontViewData record) {
        try {
            Map<String, Object> push = new HashMap<>();
            push.put("type", "front-view");

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("deviceAlias", record.getDeviceAlias());
            imagePart.put("imageIndex", record.getImageIndex());
            imagePart.put("imageData", record.getImageData());
            push.put("image", imagePart);

            Map<String, Object> dataPart = new HashMap<>();
            dataPart.put("tomatoCount", record.getTomatoCount());
            dataPart.put("recordTime", record.getRecordTime().toString());
            dataPart.put("tomatoList", record.getTomatoList());   // 已经是格式化字符串
            push.put("data", dataPart);

            String json = objectMapper.writeValueAsString(push);
            webSocketHandler.broadcast(Objects.requireNonNull(json, "json must not be null"));
            log.info("前视图推送成功：{}", json);
        } catch (Exception e) {
            log.error("前视图 WebSocket 推送失败", e);
        }
    }

    private void broadcastTopView(TopViewData record) {
        try {
            Map<String, Object> push = new HashMap<>();
            push.put("type", "top-view");

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("deviceAlias", record.getDeviceAlias());
            imagePart.put("imageIndex", record.getImageIndex());
            imagePart.put("imageData", record.getImageData());
            push.put("image", imagePart);

            Map<String, Object> dataPart = new HashMap<>();
            dataPart.put("stemDiameter", record.getStemDiameter());
            dataPart.put("recordTime", record.getRecordTime().toString());
            push.put("data", dataPart);

            String json = objectMapper.writeValueAsString(push);
            webSocketHandler.broadcast(Objects.requireNonNull(json, "json must not be null"));
            log.info("俯视图推送成功：{}", json);
        } catch (Exception e) {
            log.error("俯视图 WebSocket 推送失败", e);
        }
    }

    // ========== 工具方法 ==========
    /**
     * 从原始 List（每个元素是 Map）生成格式：fully_ripened：0.95/green：0.88/...
     */
    @SuppressWarnings("unchecked")
    private String formatTomatoListFromRaw(List<?> rawList) {
        if (rawList == null || rawList.isEmpty()) return "";
        return rawList.stream()
                .map(item -> {
                    Map<String, Object> map = (Map<String, Object>) item;
                    String ripeness = (String) map.get("ripeness");
                    Object confidenceObj = map.get("confidence");
                    String confidenceStr = confidenceObj != null ? confidenceObj.toString() : "?";
                    return ripeness + "：" + confidenceStr;
                })
                .collect(Collectors.joining("/"));
    }

    private String saveImageToLocal(byte[] imageBytes, String deviceAlias, int imageIndex, String viewType) {
        String subDir = uploadDir + File.separator + viewType;
        File dir = new File(subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String fileName = String.format("%s_%04d_%s.jpg", deviceAlias, imageIndex, timestamp);
        String filePath = subDir + File.separator + fileName;
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageBytes);
            log.info("图片已保存到: {}", filePath);
        } catch (IOException e) {
            log.error("保存图片失败: {}", filePath, e);
            throw new RuntimeException("保存图片失败: " + e.getMessage());
        }
        return filePath;
    }

    private byte[] decodeBase64Image(String base64Image) {
        try {
            String base64Data = base64Image.contains(",") ? base64Image.split(",")[1] : base64Image;
            return Base64.getDecoder().decode(base64Data);
        } catch (Exception e) {
            throw new RuntimeException("Base64解码失败: " + e.getMessage());
        }
    }

    private LocalDateTime parseTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("时间解析失败，使用当前时间: {}", timestamp);
            return LocalDateTime.now();
        }
    }
}