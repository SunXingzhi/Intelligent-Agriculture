package tech.xuexinglab.demo.demos.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;
import tech.xuexinglab.demo.demos.web.entity.PlantData;

@Mapper
public interface PlantDataMapper {

	// 插入数据
	@Insert("INSERT INTO plant_data(location, stem_diameter, tomato_count, device_id, record_time) " +
			"VALUES(#{location}, #{stemDiameter}, #{tomatoCount}, #{deviceId}, #{recordTime})")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(PlantData plantData);

	// 查询所有数据（按时间倒序）
	@Select("SELECT id, location, stem_diameter AS stemDiameter, tomato_count AS tomatoCount, " +
			"device_id AS deviceId, record_time AS recordTime FROM plant_data ORDER BY record_time DESC")
	List<PlantData> findAll();

	// 根据ID查询
	@Select("SELECT id, location, stem_diameter AS stemDiameter, tomato_count AS tomatoCount, " +
			"device_id AS deviceId, record_time AS recordTime FROM plant_data WHERE id = #{id}")
	PlantData findById(Long id);

	// 根据位置查询最新一条记录
	@Select("SELECT id, location, stem_diameter AS stemDiameter, tomato_count AS tomatoCount, " +
			"device_id AS deviceId, record_time AS recordTime FROM plant_data " +
			"WHERE location = #{location} ORDER BY record_time DESC LIMIT 1")
	PlantData findLatestByLocation(String location);

	// 更新数据
	@Update("UPDATE plant_data SET location=#{location}, stem_diameter=#{stemDiameter}, " +
			"tomato_count=#{tomatoCount}, device_id=#{deviceId} WHERE id=#{id}")
	int update(PlantData plantData);

	// 删除数据
	@Delete("DELETE FROM plant_data WHERE id = #{id}")
	int deleteById(Long id);

	// 统计每个位置的平均直径和总番茄数
	@Select("SELECT location, AVG(stem_diameter) as avgStemDiameter, SUM(tomato_count) as totalTomatoCount " +
			"FROM plant_data GROUP BY location")
	List<Map<String, Object>> getStatsByLocation();
}