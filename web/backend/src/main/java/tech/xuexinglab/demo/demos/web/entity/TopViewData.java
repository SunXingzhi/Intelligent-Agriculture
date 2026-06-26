package tech.xuexinglab.demo.demos.web.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TopViewData {
    private Long id;
    @JsonProperty("deviceAlias")
    private String deviceAlias;
    @JsonProperty("imageIndex")
    private Integer imageIndex;
    @JsonProperty("stemDiameter")
    private Double stemDiameter;
    @JsonProperty("recordTime")
    private LocalDateTime recordTime;
    @JsonProperty("imageData")
    private String imageData;            // 仅用于推送
}