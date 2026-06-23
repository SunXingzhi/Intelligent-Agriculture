package tech.xuexinglab.demo.demos.web.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FrontViewData {
    private Long id;

    @JsonProperty("deviceAlias")
    private String deviceAlias;

    @JsonProperty("imageIndex")
    private Integer imageIndex;

    @JsonProperty("tomatoCount")
    private Integer tomatoCount;

    @JsonProperty("recordTime")
    private LocalDateTime recordTime;

    // 注意：依然是 tomatoList 字段名，但类型变为 String，
    // 存储的是格式化后的成熟度字符串，如 "fully_ripened：0.95/green：0.88/..."
    @JsonProperty("tomatoList")
    private String tomatoList;

    // 仅用于 WebSocket 推送，不存入数据库
    @JsonProperty("imageData")
    private String imageData;
}