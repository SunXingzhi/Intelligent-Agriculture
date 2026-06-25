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

    @JsonProperty("tomatoList")
    private String tomatoList;

    // 仅用于 WebSocket 推送，不存入数据库
    @JsonProperty("imageData")
    private String imageData;
}