package com.kb.interact.controller;

import com.kb.common.constant.JwtConstants;
import com.kb.common.dto.CommentCreateRequest;
import com.kb.common.result.Result;
import com.kb.interact.service.CommentService;
import com.kb.interact.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InteractController {

    private final CommentService commentService;
    private final LikeService likeService;

    @PostMapping("/api/comments")
    public Result<Void> createComment(@Valid @RequestBody CommentCreateRequest request,
                                      @RequestHeader(JwtConstants.HEADER_USER_ID) Long userId) {
        commentService.createComment(request.getArticleId(), userId, request.getContent());
        return Result.success();
    }

    @PostMapping("/api/articles/{id}/like")
    public Result<Void> likeArticle(@PathVariable Long id,
                                    @RequestHeader(JwtConstants.HEADER_USER_ID) Long userId) {
        likeService.likeArticle(id, userId);
        return Result.success();
    }
}
