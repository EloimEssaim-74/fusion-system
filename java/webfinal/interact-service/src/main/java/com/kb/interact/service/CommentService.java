package com.kb.interact.service;

public interface CommentService {

    void createComment(Long articleId, Long userId, String content);
}
