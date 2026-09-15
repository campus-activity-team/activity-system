package com.example.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activity.entity.OrganizerApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrganizerApplicationMapper extends BaseMapper<OrganizerApplication> {

    @Select("SELECT * FROM organizer_applications WHERE user_id = #{userId} LIMIT 1")
    OrganizerApplication selectByUserId(Long userId);

    @Select("SELECT * FROM organizer_applications WHERE id = #{id} FOR UPDATE")
    OrganizerApplication selectByIdForUpdate(Long id);
}
