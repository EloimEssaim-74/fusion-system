package com.kb.interact.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.common.constant.RedisKeyConstants;
import com.kb.common.entity.Comment;
import com.kb.interact.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentPersistConsumer {

    private final StringRedisTemplate redisTemplate;
    private final CommentMapper commentMapper;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 5000)
    public void batchPersistComments() {
        try {
            List<String> events = new ArrayList<>();
            for (int i = 0; i < BATCH_SIZE; i++) {
                String event = redisTemplate.opsForList().rightPop(RedisKeyConstants.COMMENT_EVENTS);
                if (event == null) break;
                events.add(event);
            }

            if (events.isEmpty()) return;

            List<Comment> comments = events.stream()
                    .map(this::parseEventToComment)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!comments.isEmpty()) {
                commentMapper.insert(comments);
                log.info("Batch persisted {} comments", comments.size());
            }
        } catch (Exception e) {
            log.error("Failed to batch persist comments", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Comment parseEventToComment(String json) {
        try {
            Map<String, Object> event = objectMapper.readValue(json, Map.class);
            Comment comment = new Comment();
            comment.setArticleId(Long.valueOf(event.get("articleId").toString()));
            comment.setUserId(Long.valueOf(event.get("userId").toString()));
            comment.setContent((String) event.get("content"));

            String createdAtStr = (String) event.get("createdAt");
            if (createdAtStr != null) {
                comment.setCreatedAt(LocalDateTime.parse(createdAtStr,
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            return comment;
        } catch (Exception e) {
            log.warn("Failed to parse comment event: {}", e.getMessage());
            return null;
        }
    }
}
