package tech.xuexinglab.demo.demos.web.mapper;

import org.apache.ibatis.annotations.*;
import tech.xuexinglab.demo.demos.web.entity.User;
import java.util.List;
import java.util.Map;

@Mapper
public interface EnvironmentMapper {

    // 插入数据：列名 TotalVolatileOrganicCompounds 对应属性 totalVolatileOrganicCompounds
    @Insert("INSERT INTO tomato_data(CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
            "N, P, K, LightIntensity, TotalVolatileOrganicCompounds, recordtime) " +
            "VALUES(#{carbonConcentration}, #{temperature}, #{airHumidity}, #{soilHumidity}, " +
            "#{nitrogen}, #{phosphorus}, #{potassium}, #{lightIntensity}, #{totalVolatileOrganicCompounds}, #{recordTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    // 查询所有数据 – 使用别名映射实体 camelCase 属性
    @Select("SELECT id, CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
            "N AS nitrogen, P AS phosphorus, K AS potassium, " +
            "LightIntensity, TotalVolatileOrganicCompounds AS totalVolatileOrganicCompounds, recordtime " +
            "FROM tomato_data ORDER BY recordtime DESC")
    List<User> findAll();

    // 根据ID查询
    @Select("SELECT id, CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
            "N AS nitrogen, P AS phosphorus, K AS potassium, " +
            "LightIntensity, TotalVolatileOrganicCompounds AS totalVolatileOrganicCompounds, recordtime " +
            "FROM tomato_data WHERE id = #{id}")
    User findById(Long id);

    // 查询最新数据
    @Select("SELECT id, CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
            "N AS nitrogen, P AS phosphorus, K AS potassium, " +
            "LightIntensity, TotalVolatileOrganicCompounds AS totalVolatileOrganicCompounds, recordtime " +
            "FROM tomato_data ORDER BY recordtime DESC LIMIT 1")
    User findLatest();

    // 更新数据
    @Update("UPDATE tomato_data SET CarbonConcentration=#{carbonConcentration}, " +
            "Temperature=#{temperature}, AirHumidity=#{airHumidity}, SoilHumidity=#{soilHumidity}, " +
            "N=#{nitrogen}, P=#{phosphorus}, K=#{potassium}, " +
            "LightIntensity=#{lightIntensity}, TotalVolatileOrganicCompounds=#{totalVolatileOrganicCompounds} " +
            "WHERE id=#{id}")
    int update(User user);

    @Delete("DELETE FROM tomato_data WHERE id = #{id}")
    int deleteById(Long id);

    // 按温度范围查询
    @Select("SELECT id, CarbonConcentration, Temperature, AirHumidity, SoilHumidity, " +
            "N AS nitrogen, P AS phosphorus, K AS potassium, " +
            "LightIntensity, TotalVolatileOrganicCompounds AS totalVolatileOrganicCompounds, recordtime " +
            "FROM tomato_data WHERE Temperature BETWEEN #{min} AND #{max} ORDER BY recordtime DESC")
    List<User> findByTemperatureRange(@Param("min") double min, @Param("max") double max);

    // 统计信息（平均值保留两位小数）
    @Select("SELECT COUNT(*) as totalRecords, " +
            "ROUND(AVG(Temperature), 2) as avgTemperature, " +
            "ROUND(AVG(AirHumidity), 2) as avgAirHumidity, " +
            "ROUND(AVG(SoilHumidity), 2) as avgSoilHumidity, " +
            "ROUND(AVG(N), 2) as avgNitrogen, " +
            "ROUND(AVG(P), 2) as avgPhosphorus, " +
            "ROUND(AVG(K), 2) as avgPotassium, " +
            "ROUND(AVG(CarbonConcentration), 2) as avgCarbonConcentration, " +
            "ROUND(AVG(TotalVolatileOrganicCompounds), 2) as avgTotalVolatileOrganicCompounds, " +
            "ROUND(MAX(Temperature), 2) as maxTemperature, " +
            "ROUND(MIN(Temperature), 2) as minTemperature " +
            "FROM tomato_data")
    Map<String, Object> getStatistics();
}