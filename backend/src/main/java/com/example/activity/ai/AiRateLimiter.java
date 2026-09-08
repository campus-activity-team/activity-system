package com.example.activity.ai;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class AiRateLimiter {

    private static final Duration WINDOW_TTL = Duration.ofMinutes(2);
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AiProperties properties;

    public AiRateLimiter(StringRedisTemplate redisTemplate, AiProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void check(Long userId) {
        long minuteWindow = Instant.now().getEpochSecond() / 60;
        String key = "rate-limit:ai:activity-copy:" + userId + ":" + minuteWindow;
        try {
            Long requests = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(key),
                    String.valueOf(WINDOW_TTL.toSeconds())
            );
            if (requests == null) {
                throw new AiServiceException(503, "AI 调用频率检查暂时不可用，请稍后重试");
            }
            if (requests != null && requests > properties.getRequestsPerMinute()) {
                throw new AiServiceException(429, "AI 文案生成过于频繁，请一分钟后再试");
            }
        } catch (AiServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiServiceException(503, "AI 调用频率检查暂时不可用，请稍后重试", exception);
        }
    }
}
