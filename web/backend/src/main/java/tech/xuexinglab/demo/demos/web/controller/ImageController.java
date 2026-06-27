package tech.xuexinglab.demo.demos.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
     * 获取最新的照片序号（供树莓派查询）
     * GET /api/image/latest-index
     */
    @GetMapping("/latest-index")
    public ResponseEntity<Map<String, Object>> getLatestIndex() {
        return ResponseEntity.ok(imageService.getLatestImageIndexes());
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestBody Map<String, Object> requestData) {
        try {
            Map<String, Object> result = imageService.processDetection(requestData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}