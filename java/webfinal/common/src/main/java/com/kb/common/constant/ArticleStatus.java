package com.kb.common.constant;

import lombok.Getter;

@Getter
public enum ArticleStatus {

    DRAFT("DRAFT"),
    PUBLISHED("PUBLISHED");

    private final String value;

    ArticleStatus(String value) {
        this.value = value;
    }
}
