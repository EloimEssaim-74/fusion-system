package com.kb.common.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArticleVO {

    private Long id;
    private Long authorId;
    private String title;
    private String content;
    private String status;
    private Integer likeCount;
    private String tags;
    private String auditResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
