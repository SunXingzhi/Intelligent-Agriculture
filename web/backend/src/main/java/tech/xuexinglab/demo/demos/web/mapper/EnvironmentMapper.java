package tech.xuexinglab.demo.demos.web.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.*;
import tech.xuexinglab.demo.demos.web.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface EnvironmentMapper {
    
    // 使用驼峰命名的列名
    @Insert("INSERT INTO tomato_data(CarbonConcentration, Temperature, Humidity, " +
            "Nutrients, LightIntensity, ph, recordtime) " +
            "VALUES(#{carbonConcentration}, #{temperature}, #{humidity}, " +
            "#{nutrients}, #{lightIntensity}, #{ph}, #{recordTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    // 其他方法也需要修改列名
    @Select("SELECT * FROM tomato_data ORDER BY recordtime DESC")
    List<User> findAll();
    
    @Select("SELECT * FROM tomato_data WHERE id = #{id}")
    User findById(Long id);
    
    @Select("SELECT * FROM tomato_data ORDER BY recordtime DESC LIMIT 1")
    User findLatest();
    
    @Update("UPDATE tomato_data SET CarbonConcentration=#{carbonConcentration}, " +
            "Temperature=#{temperature}, Humidity=#{humidity}, Nutrients=#{nutrients}, " +
            "LightIntensity=#{lightIntensity}, ph=#{ph} WHERE id=#{id}")
    int update(User user);
    
    @Delete("DELETE FROM tomato_data WHERE id = #{id}")
    int deleteById(Long id);
    
    @Select("SELECT * FROM tomato_data WHERE Temperature BETWEEN #{min} AND #{max} ORDER BY recordtime DESC")
    List<User> findByTemperatureRange(@Param("min") double min, @Param("max") double max);
    
    @Select("SELECT COUNT(*) as totalRecords, " +
            "AVG(Temperature) as avgTemperature, " +
            "AVG(Humidity) as avgHumidity, " +
            "AVG(CarbonConcentration) as avgCarbonConcentration, " +
            "MAX(Temperature) as maxTemperature, " +
            "MIN(Temperature) as minTemperature " +
            "FROM tomato_data")
    Map<String, Object> getStatistics();
}