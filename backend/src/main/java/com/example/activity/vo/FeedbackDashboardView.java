package com.example.activity.vo;

import java.util.List;
import java.util.Map;

public record FeedbackDashboardView(
        Long activityId,
        String activityTitle,
        int registeredCount,
        long attendedCount,
        int feedbackCount,
        double responseRate,
        double averageOverallRating,
        double averageContentRating,
        double averageServiceRating,
        Map<Integer, Long> overallRatingDistribution,
        List<FeedbackView> feedbacks
) {
}
