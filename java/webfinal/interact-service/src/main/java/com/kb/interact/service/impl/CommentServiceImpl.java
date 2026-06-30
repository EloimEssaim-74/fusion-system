package com.kb.interact.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.common.constant.RedisKeyConstants;
import com.kb.interact.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public void createComment(Long articleId, Long userId, String content) {
        Map<String, Object> event = new HashMap<>();
        event.put("articleId", articleId);
        event.put("userId", userId);
        event.put("content", content);
        event.put("createdAt", LocalDateTime.now().toString());

        String json = objectMapper.writeValueAsString(event);

        // LPUSH to Redis list for async batch processing
        redisTemplate.opsForList().leftPush(RedisKeyConstants.COMMENT_EVENTS, json);
        log.debug("Comment event pushed to queue: articleId={}, userId={}", articleId, userId);
    }
}
