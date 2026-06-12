package tech.xuexinglab.demo.demos.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.xuexinglab.demo.demos.web.entity.ImageMessage;
import tech.xuexinglab.demo.demos.web.service.ImageService;
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
     * 接收树莓派发送的图片
     * POST /api/image/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestBody ImageMessage imageMessage) {
        try {
            // 直接调用方法，不保存返回值
            imageService.processImage(imageMessage);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "图片接收成功");
            response.put("type", imageMessage.getType());
            response.put("timestamp", imageMessage.getTimestamp());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "图片接收失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}