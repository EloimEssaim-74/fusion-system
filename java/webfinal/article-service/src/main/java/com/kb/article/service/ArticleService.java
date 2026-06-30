package com.kb.article.service;

import com.kb.common.dto.ArticleCreateRequest;
import com.kb.common.dto.ArticleUpdateRequest;
import com.kb.common.vo.ArticleListItemVO;
import com.kb.common.vo.ArticleVO;

import java.util.List;

public interface ArticleService {

    ArticleVO create(ArticleCreateRequest request, Long authorId);

    ArticleVO publish(Long articleId, Long userId, String role);

    ArticleVO update(Long articleId, ArticleUpdateRequest request, Long userId, String role);

    void delete(Long articleId, Long userId, String role);

    List<ArticleListItemVO> list(int page, int size);

    ArticleVO detail(Long articleId, Long userId);
}
