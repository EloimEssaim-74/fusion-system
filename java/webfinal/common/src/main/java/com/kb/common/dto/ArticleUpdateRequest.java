package com.kb.common.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleUpdateRequest {

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    private String content;
}
