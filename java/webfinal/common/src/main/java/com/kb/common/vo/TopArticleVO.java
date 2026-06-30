package com.kb.common.vo;

import lombok.Data;

@Data
public class TopArticleVO {

    private Long id;
    private String title;
    private Long authorId;
    private Integer likeCount;
    private Double heatScore;
}
