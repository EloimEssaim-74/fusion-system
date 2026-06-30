package com.kb.gateway.filter;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AuthPathMatcher {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/user/register",
            "/api/user/login",
            "/api/trending",
            "/actuator/health"
    );

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/trending"
    );

    public boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
