package tech.xuexinglab.demo.demos.web.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    
    @JsonProperty("CarbonConcentration")
    private Double carbonConcentration;
    
    @JsonProperty("Temperature")
    private Double temperature;
    
    @JsonProperty("Humidity")
    private Double humidity;
    
    @JsonProperty("Nutrients")
    private Double nutrients;
    
    @JsonProperty("LightIntensity")
    private Double lightIntensity;
    
    @JsonProperty("PH")  // 接收大写PH
    private Double ph;   // Java字段用小写
    
    @JsonProperty("recordTime")
    private LocalDateTime recordTime;
}