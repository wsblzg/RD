package com.example.mapper;

import com.example.entity.YcCollectionSeries;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface YcCollectionSeriesMapper {

    @Select("SELECT * FROM yc_collection_series WHERE status = 1 ORDER BY sort_no ASC, id ASC")
    List<YcCollectionSeries> selectActiveSeries();

    @Select("SELECT * FROM yc_collection_series WHERE id = #{id} LIMIT 1")
    YcCollectionSeries selectById(@Param("id") Long id);

    @Select("SELECT * FROM yc_collection_series WHERE name = #{name} LIMIT 1")
    YcCollectionSeries selectByName(@Param("name") String name);

    @Insert("INSERT INTO yc_collection_series(series_code, name, description, status, sort_no) " +
            "VALUES(#{seriesCode}, #{name}, #{description}, #{status}, #{sortNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(YcCollectionSeries series);
}
