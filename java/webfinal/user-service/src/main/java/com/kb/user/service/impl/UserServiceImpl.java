package com.kb.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kb.common.constant.JwtConstants;
import com.kb.common.constant.RedisKeyConstants;
import com.kb.common.dto.LoginRequest;
import com.kb.common.dto.RegisterRequest;
import com.kb.common.entity.User;
import com.kb.common.exception.BusinessException;
import com.kb.common.result.ResultCode;
import com.kb.common.utils.JwtUtils;
import com.kb.common.vo.UserVO;
import com.kb.user.mapper.UserMapper;
import com.kb.user.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public UserVO register(RegisterRequest request) {
        // Check username uniqueness
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null && "admin".equals(request.getRole()) ? "admin" : "user");

        userMapper.insert(user);
        log.info("User registered: id={}, username={}", user.getId(), user.getUsername());

        return toVO(user);
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        // Find user
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        // Generate JWT
        String token = JwtUtils.generateToken(user.getId(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());

        log.info("User logged in: id={}, username={}", user.getId(), user.getUsername());
        return result;
    }

    @Override
    public void logout(String token) {
        try {
            Claims claims = JwtUtils.parseToken(token);
            long remainingTtl = JwtUtils.getRemainingTtl(claims);
            if (remainingTtl > 0) {
                String blacklistKey = RedisKeyConstants.TOKEN_BLACKLIST + token;
                redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofMillis(remainingTtl));
                log.info("Token blacklisted: remainingTtl={}ms", remainingTtl);
            }
        } catch (Exception e) {
            log.warn("Logout: failed to parse token, skipping blacklist: {}", e.getMessage());
            // Even if token is expired/invalid, logout is still "successful"
        }
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
