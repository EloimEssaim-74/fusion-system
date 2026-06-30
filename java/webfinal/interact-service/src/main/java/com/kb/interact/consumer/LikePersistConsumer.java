package com.kb.interact.consumer;

import com.kb.common.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikePersistConsumer implements InitializingBean {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    private final AtomicBoolean running = new AtomicBoolean(true);

    @Override
    public void afterPropertiesSet() {
        // Create consumer group if not exists
        try {
            redisTemplate.opsForStream().createGroup(
                    RedisKeyConstants.LIKE_EVENTS_STREAM,
                    ReadOffset.from("0"),
                    RedisKeyConstants.LIKE_CONSUMER_GROUP);
            log.info("Consumer group created: {}", RedisKeyConstants.LIKE_CONSUMER_GROUP);
        } catch (Exception e) {
            // Group already exists — OK
            log.info("Consumer group already exists: {}", e.getMessage());
        }

        // Start consumer loop in a background thread
        Thread consumerThread = new Thread(this::consumeLoop, "like-persist-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("Like persist consumer started");
    }

    private void consumeLoop() {
        while (running.get()) {
            try {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                        .read(Consumer.from(RedisKeyConstants.LIKE_CONSUMER_GROUP,
                                        RedisKeyConstants.LIKE_CONSUMER_NAME),
                                StreamReadOptions.empty().count(50).block(Duration.ofSeconds(2)),
                                StreamOffset.create(RedisKeyConstants.LIKE_EVENTS_STREAM, ReadOffset.lastConsumed()));

                if (records == null || records.isEmpty()) {
                    continue;
                }

                for (MapRecord<String, Object, Object> record : records) {
                    try {
                        Map<Object, Object> value = record.getValue();
                        Long articleId = Long.valueOf(value.get("articleId").toString());

                        // Idempotent: increment like_count in MySQL
                        jdbcTemplate.update(
                                "UPDATE articles SET like_count = like_count + 1 WHERE id = ?",
                                articleId);

                        // Acknowledge message
                        redisTemplate.opsForStream().acknowledge(
                                RedisKeyConstants.LIKE_EVENTS_STREAM,
                                RedisKeyConstants.LIKE_CONSUMER_GROUP,
                                record.getId());

                        log.debug("Like persisted: articleId={}", articleId);
                    } catch (Exception e) {
                        log.error("Failed to process like event: recordId={}", record.getId(), e);
                        // Don't ack — will be retried
                    }
                }
            } catch (Exception e) {
                log.error("Like persist consumer error, retrying in 5s", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
