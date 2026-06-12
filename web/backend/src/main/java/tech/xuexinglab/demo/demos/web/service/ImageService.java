package tech.xuexinglab.demo.demos.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.xuexinglab.demo.demos.web.entity.ImageMessage;
import tech.xuexinglab.demo.demos.web.websocket.SensorWebSocketHandler;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class ImageService {
    
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    
    private final SensorWebSocketHandler webSocketHandler;
    
    public ImageService(SensorWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }
    
    /**
     * 处理接收到的图片
     */
    public String processImage(ImageMessage imageMessage) {
        // 1. 验证图片数据
        if (imageMessage.getImageData() == null || imageMessage.getImageData().isEmpty()) {
            throw new RuntimeException("图片数据不能为空");
        }
        
        if (!"front_view".equals(imageMessage.getType()) && 
            !"top_view".equals(imageMessage.getType())) {
            throw new RuntimeException("图片类型必须是 front_view 或 top_view");
        }
        
        // 2. 设置时间戳（如果没有）
        if (imageMessage.getTimestamp() == null || imageMessage.getTimestamp().isEmpty()) {
            imageMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        // 3. 解码Base64图片数据
        byte[] imageBytes = decodeBase64Image(imageMessage.getImageData());
        
        // 4. 保存图片到文件系统（可选）
        String imagePath = saveImageToFile(imageBytes, imageMessage.getType());
        log.info("图片已保存到: {}", imagePath);
        
        // 5. 通过WebSocket推送到前端
        broadcastImage(imageMessage);
        
        return "图片处理成功";
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
            String uploadDir = "uploads/images/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = imageType + "_" + timestamp + ".jpg";
            String filePath = uploadDir + fileName;
            
            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(imageBytes);
            }
            
            return filePath;
        } catch (IOException e) {
            throw new RuntimeException("保存图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 通过WebSocket广播图片
     */
    private void broadcastImage(ImageMessage imageMessage) {
        try {
            // 构造前端需要的JSON格式
            String json = String.format(
                "{\"type\":\"%s\",\"imageData\":\"%s\",\"timestamp\":\"%s\",\"deviceId\":\"%s\"}",
                imageMessage.getType(),
                imageMessage.getImageData(),
                imageMessage.getTimestamp(),
                imageMessage.getDeviceId() != null ? imageMessage.getDeviceId() : ""
            );
            
            // 推送到所有连接的前端
            webSocketHandler.broadcast(json);
            log.info("图片已推送到前端: type={}", imageMessage.getType());
            
        } catch (Exception e) {
            log.error("WebSocket推送失败", e);
        }
    }
}