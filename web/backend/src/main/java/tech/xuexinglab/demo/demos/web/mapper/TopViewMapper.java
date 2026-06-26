package tech.xuexinglab.demo.demos.web.mapper;

import org.apache.ibatis.annotations.*;
import tech.xuexinglab.demo.demos.web.entity.TopViewData;
import java.util.List;

@Mapper
public interface TopViewMapper {

    @Insert("INSERT INTO top_view_data(device_alias, image_index, record_time, stem_diameter) " +
            "VALUES(#{deviceAlias}, #{imageIndex}, #{recordTime}, #{stemDiameter})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TopViewData data);

    @Select("SELECT id, device_alias AS deviceAlias, image_index AS imageIndex, " +
            "record_time AS recordTime, stem_diameter AS stemDiameter " +
            "FROM top_view_data ORDER BY record_time DESC")
    List<TopViewData> findAll();

    @Select("SELECT id, device_alias AS deviceAlias, image_index AS imageIndex, " +
            "record_time AS recordTime, stem_diameter AS stemDiameter " +
            "FROM top_view_data WHERE id = #{id}")
    TopViewData findById(Long id);
}