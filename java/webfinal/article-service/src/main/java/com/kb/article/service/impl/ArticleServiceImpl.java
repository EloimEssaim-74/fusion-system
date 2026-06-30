package com.kb.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kb.article.config.RabbitMQConfig;
import com.kb.article.mapper.ArticleMapper;
import com.kb.article.service.ArticleService;
import com.kb.article.service.HotArticleService;
import com.kb.common.constant.ArticleStatus;
import com.kb.common.dto.ArticleCreateRequest;
import com.kb.common.dto.ArticleUpdateRequest;
import com.kb.common.entity.Article;
import com.kb.common.exception.BusinessException;
import com.kb.common.result.ResultCode;
import com.kb.common.vo.ArticleListItemVO;
import com.kb.common.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final RabbitTemplate rabbitTemplate;
    private final HotArticleService hotArticleService;

    @Override
    @Transactional
    public ArticleVO create(ArticleCreateRequest request, Long authorId) {
        Article article = new Article();
        article.setAuthorId(authorId);
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setStatus(request.getStatus() != null ? request.getStatus() : ArticleStatus.DRAFT.getValue());
        article.setDeleted(0);
        article.setLikeCount(0);

        articleMapper.insert(article);
        log.info("Article created: id={}, title={}, authorId={}", article.getId(), article.getTitle(), authorId);

        return toVO(article);
    }

    @Override
    @Transactional
    public ArticleVO publish(Long articleId, Long userId, String role) {
        Article article = findArticleOrThrow(articleId);
        checkOwnership(article, userId, role);

        if (ArticleStatus.PUBLISHED.getValue().equals(article.getStatus())) {
            throw new BusinessException("文章已发布，无需重复操作");
        }

        article.setStatus(ArticleStatus.PUBLISHED.getValue());
        articleMapper.updateById(article);

        // Send message to RabbitMQ for async processing
        Map<String, Object> message = new HashMap<>();
        message.put("articleId", article.getId());
        message.put("title", article.getTitle());
        message.put("content", article.getContent());
        message.put("authorId", article.getAuthorId());
        message.put("publishedAt", LocalDateTime.now().toString());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TOPIC_EXCHANGE,
                "article.publish",
                message);
        log.info("Article published, message sent: id={}", article.getId());

        return toVO(article);
    }

    @Override
    @Transactional
    public ArticleVO update(Long articleId, ArticleUpdateRequest request, Long userId, String role) {
        Article article = findArticleOrThrow(articleId);
        checkOwnership(article, userId, role);

        if (request.getTitle() != null) {
            article.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            article.setContent(request.getContent());
        }

        articleMapper.updateById(article);
        log.info("Article updated: id={}", articleId);

        return toVO(article);
    }

    @Override
    @Transactional
    public void delete(Long articleId, Long userId, String role) {
        Article article = findArticleOrThrow(articleId);
        checkOwnership(article, userId, role);

        articleMapper.deleteById(articleId);
        log.info("Article soft-deleted: id={}", articleId);
    }

    @Override
    public List<ArticleListItemVO> list(int page, int size) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        qw.eq(Article::getStatus, ArticleStatus.PUBLISHED.getValue())
          .orderByDesc(Article::getCreatedAt);

        Page<Article> pageResult = articleMapper.selectPage(new Page<>(page, size), qw);

        return pageResult.getRecords().stream()
                .map(this::toListItemVO)
                .collect(Collectors.toList());
    }

    @Override
    public ArticleVO detail(Long articleId, Long userId) {
        Article article = findArticleOrThrow(articleId);

        if (!ArticleStatus.PUBLISHED.getValue().equals(article.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }

        // Record read for hot articles (fire-and-forget)
        try {
            hotArticleService.recordRead(articleId, userId);
        } catch (Exception e) {
            log.warn("Failed to record read for article {}: {}", articleId, e.getMessage());
        }

        return toVO(article);
    }

    private Article findArticleOrThrow(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在");
        }
        return article;
    }

    private void checkOwnership(Article article, Long userId, String role) {
        if ("admin".equals(role)) {
            return;
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作此文章");
        }
    }

    private ArticleVO toVO(Article article) {
        if (article == null) return null;
        ArticleVO vo = new ArticleVO();
        vo.setId(article.getId());
        vo.setAuthorId(article.getAuthorId());
        vo.setTitle(article.getTitle());
        vo.setContent(article.getContent());
        vo.setStatus(article.getStatus());
        vo.setLikeCount(article.getLikeCount());
        vo.setTags(article.getTags());
        vo.setAuditResult(article.getAuditResult());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }

    private ArticleListItemVO toListItemVO(Article article) {
        ArticleListItemVO vo = new ArticleListItemVO();
        vo.setId(article.getId());
        vo.setAuthorId(article.getAuthorId());
        vo.setTitle(article.getTitle());
        vo.setLikeCount(article.getLikeCount());
        vo.setTags(article.getTags());
        vo.setCreatedAt(article.getCreatedAt());
        return vo;
    }
}
