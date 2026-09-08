package com.example.activity.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class AiRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private AiRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        AiProperties properties = new AiProperties();
        properties.setRequestsPerMinute(2);
        rateLimiter = new AiRateLimiter(redisTemplate, properties);
    }

    @Test
    void allowsRequestAtConfiguredLimit() {
        stubCounter(2L);

        assertDoesNotThrow(() -> rateLimiter.check(18L));
    }

    @Test
    void rejectsRequestAboveConfiguredLimit() {
        stubCounter(3L);

        AiServiceException exception = assertThrows(AiServiceException.class, () -> rateLimiter.check(18L));

        assertEquals(429, exception.getCode());
    }

    @Test
    void failsClosedWhenRedisDoesNotReturnACounter() {
        stubCounter(null);

        AiServiceException exception = assertThrows(AiServiceException.class, () -> rateLimiter.check(18L));

        assertEquals(503, exception.getCode());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubCounter(Long requests) {
        doReturn(requests).when(redisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        );
    }
}
