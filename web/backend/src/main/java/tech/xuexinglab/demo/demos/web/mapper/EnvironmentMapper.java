package tech.xuexinglab.demo.demos.web.mapper;

import org.apache.ibatis.annotations.*;
import tech.xuexinglab.demo.demos.web.entity.User;
import java.util.List;
import java.util.Map;

@Mapper
public interface EnvironmentMapper {

        @Insert("INSERT INTO tomato_data(CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
                        "N, P, K, LightIntensity, ph, recordtime) " +
                        "VALUES(#{carbonConcentration}, #{temperature}, #{airHumidity}, #{soilHumidity}, " +
                        "#{nitrogen}, #{phosphorus}, #{potassium}, #{lightIntensity}, #{ph}, #{recordTime})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(User user);

        // 查询所有数据 – 显式别名
        @Select("SELECT id, CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
                        "N AS nitrogen, P AS phosphorus, K AS potassium, " +
                        "LightIntensity, ph, recordtime FROM tomato_data ORDER BY recordtime DESC")
        List<User> findAll();

        // 根据ID查询 – 显式别名
        @Select("SELECT id, CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
                        "N AS nitrogen, P AS phosphorus, K AS potassium, " +
                        "LightIntensity, ph, recordtime FROM tomato_data WHERE id = #{id}")
        User findById(Long id);

        // 查询最新数据 – 显式别名
        @Select("SELECT id, CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
                        "N AS nitrogen, P AS phosphorus, K AS potassium, " +
                        "LightIntensity, ph, recordtime FROM tomato_data ORDER BY recordtime DESC LIMIT 1")
        User findLatest();

        @Update("UPDATE tomato_data SET CarbonConcentration=#{carbonConcentration}, " +
                        "Temperature=#{temperature}, AirHumidity=#{airHumidity}, SoilHumidity=#{soilHumidity}, " +
                        "N=#{nitrogen}, P=#{phosphorus}, K=#{potassium}, " +
                        "LightIntensity=#{lightIntensity}, ph=#{ph} WHERE id=#{id}")
        int update(User user);

        @Delete("DELETE FROM tomato_data WHERE id = #{id}")
        int deleteById(Long id);

        // 按温度范围查询 – 显式别名
        @Select("SELECT id, CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
                        "N AS nitrogen, P AS phosphorus, K AS potassium, " +
                        "LightIntensity, ph, recordtime FROM tomato_data WHERE Temperature BETWEEN #{min} AND #{max} ORDER BY recordtime DESC")
        List<User> findByTemperatureRange(@Param("min") double min, @Param("max") double max);

        @Select("SELECT COUNT(*) as totalRecords, " +
                        "ROUND(AVG(Temperature), 2) as avgTemperature, " +
                        "ROUND(AVG(AirHumidity), 2) as avgAirHumidity, " +
                        "ROUND(AVG(SoilHumidity), 2) as avgSoilHumidity, " +
                        "ROUND(AVG(N), 2) as avgNitrogen, " +
                        "ROUND(AVG(P), 2) as avgPhosphorus, " +
                        "ROUND(AVG(K), 2) as avgPotassium, " +
                        "ROUND(AVG(CarbonConcentration), 2) as avgCarbonConcentration, " +
                        "ROUND(MAX(Temperature), 2) as maxTemperature, " +
                        "ROUND(MIN(Temperature), 2) as minTemperature " +
                        "FROM tomato_data")
        Map<String, Object> getStatistics();
}