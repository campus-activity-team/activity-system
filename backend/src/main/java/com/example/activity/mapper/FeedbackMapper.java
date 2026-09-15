package com.example.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activity.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("SELECT * FROM feedbacks WHERE activity_id = #{activityId} AND user_id = #{userId} LIMIT 1")
    Feedback selectByActivityAndUser(Long activityId, Long userId);

    @Select("SELECT * FROM feedbacks WHERE activity_id = #{activityId} ORDER BY updated_at DESC, id DESC")
    List<Feedback> selectByActivityId(Long activityId);
}
