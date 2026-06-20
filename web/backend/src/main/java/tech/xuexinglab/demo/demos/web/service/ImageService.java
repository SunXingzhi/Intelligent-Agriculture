package tech.xuexinglab.demo.demos.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.xuexinglab.demo.demos.web.websocket.SensorWebSocketHandler;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ImageService {
    
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    
    @Value("${app.image.upload-dir:uploads/images/}")
    private String uploadDir;
    
    private final SensorWebSocketHandler webSocketHandler;
    
    // 统计数据
    private final AtomicLong totalImagesReceived = new AtomicLong(0);
    private final AtomicLong totalBytesReceived = new AtomicLong(0);
    
    public ImageService(SensorWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }
    
    /**
     * 处理Base64编码的图片（保留原有功能）
     */
    public String processImage(String base64Image, String type, String timestamp, String deviceId) {
        // 1. 验证图片数据
        if (base64Image == null || base64Image.isEmpty()) {
            throw new RuntimeException("图片数据不能为空");
        }
        
        if (!"front_view".equals(type) && !"top_view".equals(type) && !"yolov8_processed".equals(type)) {
            throw new RuntimeException("图片类型必须是 front_view, top_view 或 yolov8_processed");
        }
        
        // 2. 设置时间戳（如果没有）
        if (timestamp == null || timestamp.isEmpty()) {
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        // 3. 解码Base64图片数据
        byte[] imageBytes = decodeBase64Image(base64Image);
        
        // 4. 保存图片到文件系统
        String imagePath = saveImageToFile(imageBytes, type);
        log.info("图片已保存到: {}", imagePath);
        
        // 5. 通过WebSocket推送到前端
        broadcastImage(base64Image, type, timestamp, deviceId);
        
        return "图片处理成功";
    }
    
    /**
     * 处理图片字节流（新增功能）
     */
    public Map<String, Object> processImageStream(byte[] imageData, String imageType, 
                                                  String deviceId, String timestamp) {
        
        // 1. 验证图片数据
        validateImageData(imageData);
        
        // 2. 设置默认值
        if (timestamp == null || timestamp.isEmpty()) {
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = "unknown";
        }
        
        // 3. 更新统计
        totalImagesReceived.incrementAndGet();
        totalBytesReceived.addAndGet(imageData.length);
        
        // 4. 保存图片到文件
        String imagePath = saveImageToFile(imageData, imageType);
        
        // 5. 通过WebSocket推送到前端
        broadcastImageStream(imageData, imageType, deviceId, timestamp);
        
        // 6. 记录日志
        log.info("收到图片字节流: 类型={}, 设备={}, 大小={}bytes", 
                imageType, deviceId, imageData.length);
        
        // 7. 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "图片接收成功");
        response.put("image_path", imagePath);
        response.put("image_size", imageData.length);
        response.put("image_type", imageType);
        response.put("device_id", deviceId);
        response.put("timestamp", timestamp);
        response.put("total_received", totalImagesReceived.get());
        
        return response;
    }
    
    /**
     * 验证图片数据
     */
    private void validateImageData(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            throw new RuntimeException("图片数据不能为空");
        }
        
        // 检查最小大小（至少1KB）
        if (imageData.length < 1024) {
            log.warn("图片数据过小: {} bytes", imageData.length);
        }
        
        // 检查最大大小（限制10MB）
        if (imageData.length > 10 * 1024 * 1024) {
            throw new RuntimeException("图片数据过大，超过10MB限制");
        }
        
        // 检查JPEG文件头
        if (imageData.length > 2 && imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8) {
            log.debug("图片格式: JPEG");
        } else if (imageData.length > 8 && 
                   imageData[0] == (byte) 0x89 && imageData[1] == (byte) 0x50) {
            log.debug("图片格式: PNG");
        } else {
            log.warn("未知的图片格式");
        }
    }
    
    /**
     * 解码Base64图片
     */
    private byte[] decodeBase64Image(String base64Image) {
        try {
            // 移除可能的Base64前缀
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1];
            }
            
            return Base64.getDecoder().decode(base64Data);
        } catch (Exception e) {
            throw new RuntimeException("Base64解码失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存图片到文件
     */
    private String saveImageToFile(byte[] imageBytes, String imageType) {
        try {
            // 创建保存目录
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String fileName = imageType + "_" + timestamp + ".jpg";
            String filePath = uploadDir + fileName;
            
            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(imageBytes);
            }
            
            log.info("图片保存到: {}", filePath);
            return filePath;
            
        } catch (IOException e) {
            throw new RuntimeException("保存图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 通过WebSocket广播图片（Base64版本）
     */
    private void broadcastImage(String base64Image, String type, String timestamp, String deviceId) {
        try {
            // 构造JSON消息
            String json = String.format(
                "{\"type\":\"%s\",\"imageData\":\"%s\",\"timestamp\":\"%s\",\"deviceId\":\"%s\"}",
                type,
                base64Image,
                timestamp,
                deviceId != null ? deviceId : ""
            );
            
            // 推送到所有连接的前端
            webSocketHandler.broadcast(json);
            log.info("图片已推送到前端: type={}", type);
            
        } catch (Exception e) {
            log.error("WebSocket推送失败", e);
        }
    }
    
    /**
     * 通过WebSocket广播图片字节流（新增）
     */
    private void broadcastImageStream(byte[] imageData, String imageType, 
                                     String deviceId, String timestamp) {
        try {
            // 将字节流转换为Base64（用于WebSocket传输）
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            
            // 构造JSON消息
            String json = String.format(
                "{\"type\":\"%s\",\"imageData\":\"%s\",\"timestamp\":\"%s\"," +
                "\"deviceId\":\"%s\",\"imageSize\":%d}",
                imageType,
                base64Image,
                timestamp,
                deviceId,
                imageData.length
            );
            
            // 推送到前端
            webSocketHandler.broadcast(json);
            log.debug("图片已推送到前端: 类型={}, 大小={}bytes", imageType, imageData.length);
            
        } catch (Exception e) {
            log.error("WebSocket推送失败", e);
        }
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_images_received", totalImagesReceived.get());
        stats.put("total_bytes_received", totalBytesReceived.get());
        stats.put("average_image_size", 
                  totalImagesReceived.get() > 0 ? 
                  (double) totalBytesReceived.get() / totalImagesReceived.get() : 0.0);
        stats.put("timestamp", LocalDateTime.now().toString());
        
        return stats;
    }
}