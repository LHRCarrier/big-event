package com.bubbles.server.mapper;

import com.bubbles.pojo.entity.PartitionMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PartitionMappingMapper {

    @Select("select partition_tag from partition_mapping where tname = #{tname}")
    String getPartitionByTname(String tname);

    @Select("select id, tname, partition_tag, created_at from partition_mapping")
    List<PartitionMapping> listAll();
}
