package com.bubbles.server.mapper;

import com.bubbles.pojo.entity.HotSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface HotSnapshotMapper {

    void batchInsert(List<HotSnapshot> snapshots);

    /**
     * 查询最近60天内的所有快照（用于评分计算）
     */
    List<HotSnapshot> getByTimeRange(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 获取最新的快照批次时间
     */
    @Select("select max(snapshot_time) from hot_snapshot")
    LocalDateTime getLatestSnapshotTime();

    /**
     * 根据 bvid 获取最新一条快照
     */
    @Select("select * from hot_snapshot where bvid = #{bvid} order by snapshot_time desc limit 1")
    HotSnapshot getLatestByBvid(@Param("bvid") String bvid);
}
