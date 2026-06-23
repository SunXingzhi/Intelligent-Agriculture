package tech.xuexinglab.demo.demos.web.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TomatoInfo {
    @JsonProperty("ripeness")
    private String ripeness;          // fully_ripened / green / half_ripened
    @JsonProperty("confidence")
    private Double confidence;        // 0.0 ~ 1.0
}