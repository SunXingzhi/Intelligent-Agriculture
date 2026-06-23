package tech.xuexinglab.demo.demos.web.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PlantData {
    private Long id;
    @JsonProperty("location")
    private String location;            // 位置
    @JsonProperty("stemDiameter")
    private Double stemDiameter;        // 植株直径（mm）
    @JsonProperty("tomatoCount")
    private Integer tomatoCount;        // 番茄数量
    @JsonProperty("deviceId")
    private LocalDateTime recordTime;   // 记录时间
}