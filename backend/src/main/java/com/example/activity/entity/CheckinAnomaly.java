package com.example.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("checkin_anomalies")
public class CheckinAnomaly {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private CheckinAnomalyReason reason;
    private String message;
    private Double latitude;
    private Double longitude;
    private Integer distanceMeters;
    private LocalDateTime createdAt;
}
