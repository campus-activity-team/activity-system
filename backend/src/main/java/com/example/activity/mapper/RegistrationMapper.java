package com.example.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activity.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    @Select("SELECT * FROM registrations WHERE activity_id = #{activityId} AND user_id = #{userId} LIMIT 1")
    Registration selectByActivityAndUser(Long activityId, Long userId);
}
