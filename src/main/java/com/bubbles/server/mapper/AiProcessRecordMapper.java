package com.bubbles.server.mapper;

import com.bubbles.pojo.entity.AiProcessRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiProcessRecordMapper {

    @Insert("insert into ai_process_record(bvid, source, source_id, article_id, process_time, status, ai_model, hot_score) " +
            "values(#{bvid}, #{source}, #{sourceId}, #{articleId}, #{processTime}, #{status}, #{aiModel}, #{hotScore})")
    void insert(AiProcessRecord record);

    @Select("select count(*) from ai_process_record where source = #{source} and source_id = #{sourceId}")
    int countBySourceId(@Param("source") String source, @Param("sourceId") String sourceId);

    /** @deprecated use countBySourceId instead */
    @Deprecated
    @Select("select count(*) from ai_process_record where bvid = #{bvid}")
    int countByBvid(String bvid);
}
