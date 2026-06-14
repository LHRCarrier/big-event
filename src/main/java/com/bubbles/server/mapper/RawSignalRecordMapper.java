package com.bubbles.server.mapper;

import com.bubbles.pojo.entity.RawSignalRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RawSignalRecordMapper {

    @Insert("insert into raw_signal(source, source_id, title, url, author, cover_url, category, " +
            "partition_tag, `rank`, raw_metrics, norm_score, fetched_at) " +
            "values(#{source}, #{sourceId}, #{title}, #{url}, #{author}, #{coverUrl}, #{category}, " +
            "#{partitionTag}, #{rank}, #{rawMetrics}, #{normScore}, #{fetchedAt})")
    void insert(RawSignalRecord record);

    @Select("select * from raw_signal where source = #{source} and source_id = #{sourceId} " +
            "order by fetched_at desc")
    List<RawSignalRecord> getBySourceId(@Param("source") String source,
                                         @Param("sourceId") String sourceId);

    @Select("select * from raw_signal where fetched_at between #{start} and #{end} " +
            "order by fetched_at desc")
    List<RawSignalRecord> getByTimeRange(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Select("select * from raw_signal where source = #{source} " +
            "and fetched_at between #{start} and #{end} order by fetched_at desc")
    List<RawSignalRecord> getBySourceAndTimeRange(@Param("source") String source,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    @Select("select count(*) from raw_signal where source = #{source} and source_id = #{sourceId}")
    int countBySourceId(@Param("source") String source, @Param("sourceId") String sourceId);
}
