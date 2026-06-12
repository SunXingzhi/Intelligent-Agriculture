package tech.xuexinglab.demo.demos.web.entity;

import lombok.Data;
// import java.time.LocalDateTime;

@Data
public class ImageMessage {
    private String type;  // "top_view" 或 "front_view"
    private String imageData;  // Base64编码的图片数据
    private String timestamp;  // 时间戳
    private String deviceId;  // 设备ID
}