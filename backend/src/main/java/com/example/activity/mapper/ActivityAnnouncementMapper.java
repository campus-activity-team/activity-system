package com.example.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activity.entity.ActivityAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActivityAnnouncementMapper extends BaseMapper<ActivityAnnouncement> {

    @Select("SELECT * FROM activity_announcements WHERE activity_id = #{activityId} ORDER BY created_at DESC, id DESC")
    List<ActivityAnnouncement> selectByActivityId(Long activityId);
}
