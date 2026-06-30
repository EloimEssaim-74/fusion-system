package com.kb.common.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArticleListItemVO {

    private Long id;
    private Long authorId;
    private String title;
    private Integer likeCount;
    private String tags;
    private LocalDateTime createdAt;
}
