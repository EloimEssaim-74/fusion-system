package com.kb.article.controller;

import com.kb.article.service.ArticleService;
import com.kb.article.service.HotArticleService;
import com.kb.common.constant.JwtConstants;
import com.kb.common.dto.ArticleCreateRequest;
import com.kb.common.dto.ArticleUpdateRequest;
import com.kb.common.result.Result;
import com.kb.common.vo.ArticleListItemVO;
import com.kb.common.vo.ArticleVO;
import com.kb.common.vo.TopArticleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final HotArticleService hotArticleService;

    @PostMapping("/api/articles")
    public Result<ArticleVO> create(@Valid @RequestBody ArticleCreateRequest request,
                                    @RequestHeader(JwtConstants.HEADER_USER_ID) Long userId) {
        ArticleVO article = articleService.create(request, userId);
        return Result.success(article);
    }

    @PutMapping("/api/articles/{id}/publish")
    public Result<ArticleVO> publish(@PathVariable Long id,
                                     @RequestHeader(JwtConstants.HEADER_USER_ID) Long userId,
                                     @RequestHeader(JwtConstants.HEADER_USER_ROLE) String role) {
        ArticleVO article = articleService.publish(id, userId, role);
        return Result.success(article);
    }

    @PutMapping("/api/articles/{id}")
    public Result<ArticleVO> update(@PathVariable Long id,
                                    @Valid @RequestBody ArticleUpdateRequest request,
                                    @RequestHeader(JwtConstants.HEADER_USER_ID) Long userId,
                                    @RequestHeader(JwtConstants.HEADER_USER_ROLE) String role) {
        ArticleVO article = articleService.update(id, request, userId, role);
        return Result.success(article);
    }

    @DeleteMapping("/api/articles/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader(JwtConstants.HEADER_USER_ID) Long userId,
                               @RequestHeader(JwtConstants.HEADER_USER_ROLE) String role) {
        articleService.delete(id, userId, role);
        return Result.success();
    }

    @GetMapping("/api/articles")
    public Result<List<ArticleListItemVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ArticleListItemVO> articles = articleService.list(page, size);
        return Result.success(articles);
    }

    @GetMapping("/api/articles/{id}")
    public Result<ArticleVO> detail(@PathVariable Long id,
                                    @RequestHeader(value = JwtConstants.HEADER_USER_ID, defaultValue = "0") Long userId) {
        ArticleVO article = articleService.detail(id, userId);
        return Result.success(article);
    }

    @GetMapping("/api/trending")
    public Result<List<TopArticleVO>> trending() {
        List<TopArticleVO> top10 = hotArticleService.getTop10();
        return Result.success(top10);
    }
}
