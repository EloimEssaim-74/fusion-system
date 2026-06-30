package com.kb.interact.service.impl;

import com.kb.common.constant.RedisKeyConstants;
import com.kb.common.exception.BusinessException;
import com.kb.interact.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void likeArticle(Long articleId, Long userId) {
        // 1. Dedup check — one like per user per article (permanent)
        String dedupKey = String.format(RedisKeyConstants.LIKE_DEDUP, articleId, userId);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1");
        if (Boolean.FALSE.equals(success)) {
            throw new BusinessException("您已点赞过该文章");
        }

        // 2. Write to Redis Stream for async persistence
        Map<String, String> event = new HashMap<>();
        event.put("articleId", String.valueOf(articleId));
        event.put("userId", String.valueOf(userId));
        event.put("timestamp", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForStream().add(RedisKeyConstants.LIKE_EVENTS_STREAM, event);

        // 3. Update hot articles ZSET (+3 for a like)
        redisTemplate.opsForZSet().incrementScore(
                RedisKeyConstants.HOT_ARTICLES,
                String.valueOf(articleId),
                3);

        log.debug("Like recorded: articleId={}, userId={}", articleId, userId);
    }
}
