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
     * 接收已识别的 JSON，不包含 type 字段，通过 deviceAlias 前缀自动区分视图
     */
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