package com.bubbles.server.mapper;

import com.bubbles.pojo.entity.Topic;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TopicMapper {

    @Insert("insert into topic(title, aliases, keywords, first_seen_at, last_seen_at, status) " +
            "values(#{title}, #{aliases}, #{keywords}, #{firstSeenAt}, #{lastSeenAt}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Topic topic);

    @Select("select * from topic where id = #{id}")
    Topic getById(Long id);

    @Select("select * from topic where status = 'active' order by last_seen_at desc")
    List<Topic> getActiveTopics();

    @Update("update topic set last_seen_at = #{lastSeenAt}, status = #{status} where id = #{id}")
    void updateStatus(Topic topic);

    @Update("update topic set status = 'merged', merged_into = #{mergedInto} where id = #{id}")
    void mergeInto(@Param("id") Long id, @Param("mergedInto") Long mergedInto);
}
