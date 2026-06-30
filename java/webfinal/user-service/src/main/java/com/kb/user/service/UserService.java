package com.kb.user.service;

import com.kb.common.dto.LoginRequest;
import com.kb.common.dto.RegisterRequest;
import com.kb.common.vo.UserVO;

import java.util.Map;

public interface UserService {

    UserVO register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    void logout(String token);
}
