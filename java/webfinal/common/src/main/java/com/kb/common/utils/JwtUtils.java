package com.kb.common.utils;

import com.kb.common.constant.JwtConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            JwtConstants.SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + JwtConstants.EXPIRATION_MS);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(KEY)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public static String getUserRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public static boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public static long getRemainingTtl(Claims claims) {
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }
}
