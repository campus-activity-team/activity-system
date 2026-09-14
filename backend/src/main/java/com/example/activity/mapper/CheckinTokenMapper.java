package com.example.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.activity.entity.CheckinToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CheckinTokenMapper extends BaseMapper<CheckinToken> {

    @Select("SELECT * FROM checkin_tokens WHERE token = #{token} LIMIT 1")
    CheckinToken selectByToken(String token);
}
