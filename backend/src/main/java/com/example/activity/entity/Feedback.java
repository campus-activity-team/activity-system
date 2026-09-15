package com.example.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedbacks")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private Integer overallRating;
    private Integer contentRating;
    private Integer serviceRating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
