package com.example.activity.controller;

import com.example.activity.common.ApiResponse;
import com.example.activity.dto.FeedbackRequest;
import com.example.activity.service.FeedbackService;
import com.example.activity.vo.FeedbackStatusView;
import com.example.activity.vo.FeedbackView;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/activities/{activityId}")
    public ApiResponse<FeedbackStatusView> mine(
            @PathVariable Long activityId,
            Authentication authentication
    ) {
        return ApiResponse.success(feedbackService.mine(activityId, authentication));
    }

    @PutMapping("/activities/{activityId}")
    public ApiResponse<FeedbackView> submit(
            @PathVariable Long activityId,
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(feedbackService.submit(activityId, request, authentication));
    }
}
