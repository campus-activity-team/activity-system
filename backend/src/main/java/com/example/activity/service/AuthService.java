package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.dto.LoginRequest;
import com.example.activity.dto.RegisterRequest;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.security.JwtService;
import com.example.activity.vo.LoginResponse;
import com.example.activity.vo.UserProfile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserProfile register(RegisterRequest request) {
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username())) > 0) {
            throw new BusinessException(409, "用户名已存在");
        }
        if (request.studentId() != null && !request.studentId().isBlank()
                && userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getStudentId, request.studentId())) > 0) {
            throw new BusinessException(409, "学号已存在");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name().trim());
        user.setStudentId(blankToNull(request.studentId()));
        user.setEmail(blankToNull(request.email()));
        user.setPhone(blankToNull(request.phone()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);
        return UserProfile.from(user);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) userDetails;
        return new LoginResponse(
                jwtService.generateToken(userDetails),
                jwtService.getExpirationSeconds(),
                UserProfile.from(authenticatedUser.user())
        );
    }

    public UserProfile currentUser(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return UserProfile.from(user.user());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
