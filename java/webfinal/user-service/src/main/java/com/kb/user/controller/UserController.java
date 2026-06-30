package com.kb.user.controller;

import com.kb.common.constant.JwtConstants;
import com.kb.common.dto.LoginRequest;
import com.kb.common.dto.RegisterRequest;
import com.kb.common.result.Result;
import com.kb.common.vo.UserVO;
import com.kb.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        UserVO user = userService.register(request);
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = userService.login(request);
        return Result.success(result);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(JwtConstants.HEADER_NAME) String authHeader) {
        String token = authHeader;
        if (authHeader.startsWith(JwtConstants.TOKEN_PREFIX)) {
            token = authHeader.substring(JwtConstants.TOKEN_PREFIX.length()).trim();
        }
        userService.logout(token);
        return Result.success();
    }
}
