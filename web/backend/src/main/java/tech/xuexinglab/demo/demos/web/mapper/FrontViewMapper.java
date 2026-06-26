package tech.xuexinglab.demo.demos.web.mapper;

import org.apache.ibatis.annotations.*;
import tech.xuexinglab.demo.demos.web.entity.FrontViewData;
import java.util.List;

@Mapper
public interface FrontViewMapper {

    // 插入时，tomato_list 列对应实体中的 tomatoList 属性
    @Insert("INSERT INTO front_view_data(device_alias, image_index, tomato_count, tomato_list, record_time) " +
            "VALUES(#{deviceAlias}, #{imageIndex}, #{tomatoCount}, #{tomatoList}, #{recordTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FrontViewData data);

    // 查询时，将 tomato_list 列映射到 tomatoList 属性
    @Select("SELECT id, device_alias AS deviceAlias, image_index AS imageIndex, " +
            "tomato_count AS tomatoCount, tomato_list AS tomatoList, " +
            "record_time AS recordTime FROM front_view_data ORDER BY record_time DESC")
    List<FrontViewData> findAll();

    @Select("SELECT id, device_alias AS deviceAlias, image_index AS imageIndex, " +
            "tomato_count AS tomatoCount, tomato_list AS tomatoList, " +
            "record_time AS recordTime FROM front_view_data WHERE id = #{id}")
    FrontViewData findById(Long id);
}