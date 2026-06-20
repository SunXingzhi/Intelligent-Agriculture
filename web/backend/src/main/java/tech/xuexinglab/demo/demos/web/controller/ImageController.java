package tech.xuexinglab.demo.demos.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.xuexinglab.demo.demos.web.service.ImageService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/image")
public class ImageController {
    
    private final ImageService imageService;
    
    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }
    
    /**
     * 接收Base64编码的图片（保留原有功能）
     * POST /api/image/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestBody Map<String, Object> requestData) {
        try {
            String imageData = (String) requestData.get("imageData");
            String type = (String) requestData.get("type");
            String timestamp = (String) requestData.get("timestamp");
            String deviceId = (String) requestData.get("deviceId");
            
            String result = imageService.processImage(imageData, type, timestamp, deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "上传失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 接收YOLOv8处理后的图片字节流（新增功能）
     * POST /api/image/stream
     */
    @PostMapping("/stream")
    public ResponseEntity<Map<String, Object>> receiveImageStream(
            @RequestBody byte[] imageData,
            @RequestHeader(value = "X-Image-Type", defaultValue = "yolov8_processed") String imageType,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp) {
        
        try {
            // 调用Service处理字节流
            Map<String, Object> result = imageService.processImageStream(
                imageData, imageType, deviceId, timestamp);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "图片处理失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}