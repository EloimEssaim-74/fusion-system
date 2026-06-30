package com.kb.article.service;

import com.kb.common.vo.TopArticleVO;

import java.util.List;

public interface HotArticleService {

    void recordRead(Long articleId, Long userId);

    void recordLike(Long articleId);

    List<TopArticleVO> getTop10();
}
