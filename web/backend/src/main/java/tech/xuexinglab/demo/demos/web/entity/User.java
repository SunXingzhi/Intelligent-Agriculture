package tech.xuexinglab.demo.demos.web.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    
    @JsonProperty("CarbonConcentration")
    private Double carbonConcentration;  // 二氧化碳浓度(%)
    
    @JsonProperty("Temperature")
    private Double temperature;          // 温度(℃)
    
    @JsonProperty("AirHumidity")
    private Double airHumidity;          // 空气湿度(%)
    
    @JsonProperty("SoilHumidity")
    private Double soilHumidity;         // 土壤湿度(%)
    
    @JsonProperty("Nitrogen")
    private Double nitrogen;             // 氮含量(mg/L)
    
    @JsonProperty("Phosphorus")
    private Double phosphorus;           // 磷含量(mg/L)
    
    @JsonProperty("Potassium")
    private Double potassium;            // 钾含量(mg/L)
    
    @JsonProperty("LightIntensity")
    private Double lightIntensity;       // 光强(lux)
    
    @JsonProperty("PH")
    private Double ph;                   // pH值
    
    @JsonProperty("recordTime")
    private LocalDateTime recordTime;    // 记录时间
}