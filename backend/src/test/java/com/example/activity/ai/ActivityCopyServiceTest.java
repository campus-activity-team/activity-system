package com.example.activity.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityCopyServiceTest {

    private DeepSeekClient deepSeekClient;
    private AiRateLimiter rateLimiter;
    private ActivityCopyService service;

    @BeforeEach
    void setUp() {
        deepSeekClient = mock(DeepSeekClient.class);
        rateLimiter = mock(AiRateLimiter.class);
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-api-key");
        properties.setModel("deepseek-chat");
        service = new ActivityCopyService(deepSeekClient, new ObjectMapper(), properties, rateLimiter);
    }

    @Test
    void parsesStructuredCopyAndLimitsListSizes() {
        when(deepSeekClient.generateActivityCopy(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
                {
                  "title": "春季创新实践周",
                  "description": "面向高校学生的创新实践活动。",
                  "highlights": ["主题分享", "实践交流", "成果展示"],
                  "notices": ["请提前报名"],
                  "tags": ["创新", "实践"]
                }
                """);

        ActivityCopyResponse result = service.generate(
                new ActivityCopyRequest("创新实践", "活动周", "高校学生", "校内", "周末", "创新", "正式"),
                12L
        );

        assertEquals("春季创新实践周", result.title());
        assertEquals(3, result.highlights().size());
        assertEquals("deepseek-chat", result.model());
        verify(rateLimiter).check(12L);
    }

    @Test
    void acceptsJsonInsideMarkdownCodeFence() {
        when(deepSeekClient.generateActivityCopy(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
                ```json
                {"title":"活动标题","description":"活动介绍","highlights":[],"notices":[],"tags":[]}
                ```
                """);

        ActivityCopyResponse result = service.generate(
                new ActivityCopyRequest("主题", null, null, null, null, null, null),
                1L
        );

        assertEquals("活动标题", result.title());
    }

    @Test
    void rejectsResponseWithoutRequiredDescription() {
        when(deepSeekClient.generateActivityCopy(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                "{\"title\":\"只有标题\",\"highlights\":[],\"notices\":[],\"tags\":[]}"
        );

        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> service.generate(new ActivityCopyRequest("主题", null, null, null, null, null, null), 1L)
        );

        assertEquals(502, exception.getCode());
    }
}
