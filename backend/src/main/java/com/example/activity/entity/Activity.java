package com.example.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activities")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private Long organizerId;
    private String location;
    private Double checkinLatitude;
    private Double checkinLongitude;
    private Integer checkinRadiusMeters;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registrationStartTime;
    private LocalDateTime registrationEndTime;
    private Integer capacity;
    private Integer currentRegisteredCount;
    private ActivityStatus status;
    private String reviewComment;
    private Boolean requireFeedback;
    private LocalDateTime feedbackDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
