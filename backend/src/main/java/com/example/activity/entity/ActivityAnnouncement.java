package com.example.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_announcements")
public class ActivityAnnouncement {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long publisherId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
