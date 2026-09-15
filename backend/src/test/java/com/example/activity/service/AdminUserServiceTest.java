package com.example.activity.service;

import com.example.activity.dto.AdminRoleChangeRequest;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private OperationLogService operationLogService;
    private AdminUserService adminUserService;
    private User administrator;
    private Authentication administratorAuthentication;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        operationLogService = mock(OperationLogService.class);
        adminUserService = new AdminUserService(userMapper, passwordEncoder, operationLogService);

        administrator = user(1L, UserRole.ADMIN);
        administrator.setPasswordHash("encoded-password");
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(administrator);
        administratorAuthentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
    }

    @Test
    void promotesRegisteredUserToAdministratorAfterPasswordConfirmation() {
        User target = user(8L, UserRole.USER);
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);
        when(userMapper.selectById(8L)).thenReturn(target);

        var result = adminUserService.changeRole(
                8L,
                new AdminRoleChangeRequest(UserRole.ADMIN, "correct-password"),
                administratorAuthentication
        );

        assertEquals(UserRole.ADMIN, result.role());
        verify(userMapper).updateById(target);
        verify(operationLogService).record(1L, "USER_ROLE_CHANGED_USER_TO_ADMIN", "USER", 8L);
    }

    @Test
    void rejectsRoleChangeWithIncorrectAdministratorPassword() {
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.changeRole(
                        8L,
                        new AdminRoleChangeRequest(UserRole.ADMIN, "wrong-password"),
                        administratorAuthentication
                )
        );

        assertEquals(403, exception.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void rejectsChangingOwnAdministratorRole() {
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.changeRole(
                        1L,
                        new AdminRoleChangeRequest(UserRole.USER, "correct-password"),
                        administratorAuthentication
                )
        );

        assertEquals(409, exception.getCode());
    }

    @Test
    void keepsAtLeastOneActiveAdministrator() {
        User target = user(9L, UserRole.ADMIN);
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);
        when(userMapper.selectById(9L)).thenReturn(target);
        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> adminUserService.changeRole(
                        9L,
                        new AdminRoleChangeRequest(UserRole.USER, "correct-password"),
                        administratorAuthentication
                )
        );

        assertEquals(409, exception.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setName("用户 " + id);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
