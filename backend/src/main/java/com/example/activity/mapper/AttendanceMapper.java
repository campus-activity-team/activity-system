package com.example.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activity.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

    @Select("SELECT * FROM attendances WHERE activity_id = #{activityId} AND user_id = #{userId} LIMIT 1")
    Attendance selectByActivityAndUser(Long activityId, Long userId);

    @Select("SELECT COUNT(*) FROM attendances WHERE activity_id = #{activityId} AND status = 'SUCCESS'")
    long countSuccessfulByActivityId(Long activityId);
}
