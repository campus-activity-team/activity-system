package com.example.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checkin_tokens")
public class CheckinToken {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String token;
    private Long activityId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
