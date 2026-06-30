package com.kb.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.common.constant.JwtConstants;
import com.kb.common.constant.RedisKeyConstants;
import com.kb.common.result.Result;
import com.kb.common.result.ResultCode;
import com.kb.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthPathMatcher pathMatcher;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Skip authentication for public paths
        if (pathMatcher.isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. Extract token from Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(JwtConstants.HEADER_NAME);
        if (authHeader == null || !authHeader.startsWith(JwtConstants.TOKEN_PREFIX)) {
            return writeError(exchange, ResultCode.UNAUTHORIZED, "缺少或无效的认证令牌");
        }
        String token = authHeader.substring(JwtConstants.TOKEN_PREFIX.length()).trim();

        // 3. Parse and validate JWT signature/expiry
        Claims claims;
        try {
            claims = JwtUtils.parseToken(token);
        } catch (ExpiredJwtException e) {
            return writeError(exchange, ResultCode.UNAUTHORIZED, "令牌已过期");
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            return writeError(exchange, ResultCode.UNAUTHORIZED, "无效的令牌");
        }

        // 4. Check Redis blacklist
        String blacklistKey = RedisKeyConstants.TOKEN_BLACKLIST + token;
        return redisTemplate.hasKey(blacklistKey)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        return writeError(exchange, ResultCode.UNAUTHORIZED, "令牌已注销");
                    }

                    Long userId = JwtUtils.getUserId(claims);
                    String role = JwtUtils.getUserRole(claims);

                    // 5. Role-based access control: /admin/* requires admin role
                    if (path.startsWith("/admin") && !"admin".equals(role)) {
                        return writeError(exchange, ResultCode.FORBIDDEN, "需要管理员权限");
                    }

                    // 6. Propagate user identity to downstream services via headers
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header(JwtConstants.HEADER_USER_ID, String.valueOf(userId))
                            .header(JwtConstants.HEADER_USER_ROLE, role)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, ResultCode code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.valueOf(code.getCode()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(Result.error(code, message));
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":500,\"message\":\"internal error\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
