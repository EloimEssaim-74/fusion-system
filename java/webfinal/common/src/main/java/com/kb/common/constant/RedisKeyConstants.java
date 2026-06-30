package com.kb.common.constant;

public class RedisKeyConstants {

    public static final String TOKEN_BLACKLIST = "token:blacklist:";

    public static final String READ_DEDUP = "read:article:%d:user:%d";
    public static final int READ_DEDUP_TTL_SECONDS = 300;

    public static final String LIKE_DEDUP = "like:article:%d:user:%d";

    public static final String HOT_ARTICLES = "hot_articles";

    public static final String COMMENT_EVENTS = "comment:events";

    public static final String LIKE_EVENTS_STREAM = "like:events";
    public static final String LIKE_CONSUMER_GROUP = "like-persist-group";
    public static final String LIKE_CONSUMER_NAME = "like-persist-consumer";

    private RedisKeyConstants() {}
}
