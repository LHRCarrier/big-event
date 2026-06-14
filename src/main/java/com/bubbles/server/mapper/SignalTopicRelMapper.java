package com.bubbles.server.mapper;

import com.bubbles.pojo.entity.SignalTopicRel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SignalTopicRelMapper {

    @Insert("insert into signal_topic_rel(signal_id, topic_id, confidence) " +
            "values(#{signalId}, #{topicId}, #{confidence})")
    void insert(SignalTopicRel rel);

    @Select("select * from signal_topic_rel where topic_id = #{topicId}")
    List<SignalTopicRel> getByTopicId(@Param("topicId") Long topicId);

    @Select("select * from signal_topic_rel where signal_id = #{signalId}")
    List<SignalTopicRel> getBySignalId(@Param("signalId") Long signalId);
}
