package com.kb.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.article.mapper.ArticleMapper;
import com.kb.article.service.HotArticleService;
import com.kb.common.constant.RedisKeyConstants;
import com.kb.common.entity.Article;
import com.kb.common.vo.TopArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleServiceImpl implements HotArticleService {

    private final StringRedisTemplate redisTemplate;
    private final ArticleMapper articleMapper;

    @Override
    public void recordRead(Long articleId, Long userId) {
        // Dedup: same user reads same article within 5 minutes counts only once
        String dedupKey = String.format(RedisKeyConstants.READ_DEDUP, articleId, userId);
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(RedisKeyConstants.READ_DEDUP_TTL_SECONDS));

        if (Boolean.TRUE.equals(isNew)) {
            redisTemplate.opsForZSet().incrementScore(
                    RedisKeyConstants.HOT_ARTICLES,
                    String.valueOf(articleId),
                    1);
            log.debug("Hot article read recorded: articleId={}, score +1", articleId);
        }
    }

    @Override
    public void recordLike(Long articleId) {
        redisTemplate.opsForZSet().incrementScore(
                RedisKeyConstants.HOT_ARTICLES,
                String.valueOf(articleId),
                3);
        log.debug("Hot article like recorded: articleId={}, score +3", articleId);
    }

    @Override
    public List<TopArticleVO> getTop10() {
        Set<ZSetOperations.TypedTuple<String>> top =
                redisTemplate.opsForZSet().reverseRangeWithScores(
                        RedisKeyConstants.HOT_ARTICLES, 0, 9);

        if (top == null || top.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect article IDs to batch-fetch metadata
        List<Long> articleIds = top.stream()
                .map(t -> Long.valueOf(Objects.requireNonNull(t.getValue())))
                .collect(Collectors.toList());

        // Batch fetch articles from MySQL
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().in(Article::getId, articleIds));

        Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a, (a, b) -> a));

        // Build result preserving ZSET order
        List<TopArticleVO> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> t : top) {
            Long articleId = Long.valueOf(Objects.requireNonNull(t.getValue()));
            Article article = articleMap.get(articleId);
            if (article != null) {
                TopArticleVO vo = new TopArticleVO();
                vo.setId(article.getId());
                vo.setTitle(article.getTitle());
                vo.setAuthorId(article.getAuthorId());
                vo.setLikeCount(article.getLikeCount());
                vo.setHeatScore(t.getScore());
                result.add(vo);
            }
        }

        return result;
    }
}
