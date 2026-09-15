package com.example.activity.service;

import com.example.activity.dto.OrganizerApplicationRequest;
import com.example.activity.entity.OrganizerApplication;
import com.example.activity.entity.OrganizerApplicationStatus;
import com.example.activity.entity.User;
import com.example.activity.entity.UserRole;
import com.example.activity.entity.UserStatus;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.OrganizerApplicationMapper;
import com.example.activity.mapper.UserMapper;
import com.example.activity.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizerApplicationServiceTest {

    private OrganizerApplicationMapper organizerApplicationMapper;
    private UserMapper userMapper;
    private OperationLogService operationLogService;
    private OrganizerApplicationService organizerApplicationService;
    private User applicant;
    private Authentication applicantAuthentication;
    private User administrator;
    private Authentication administratorAuthentication;

    @BeforeEach
    void setUp() {
        organizerApplicationMapper = mock(OrganizerApplicationMapper.class);
        userMapper = mock(UserMapper.class);
        operationLogService = mock(OperationLogService.class);
        organizerApplicationService = new OrganizerApplicationService(
                organizerApplicationMapper,
                userMapper,
                operationLogService
        );

        applicant = user(8L, "student", UserRole.USER);
        applicantAuthentication = authentication(applicant);
        administrator = user(1L, "admin", UserRole.ADMIN);
        administratorAuthentication = authentication(administrator);
    }

    @Test
    void userCanSubmitOrganizerApplication() {
        when(organizerApplicationMapper.selectByUserId(8L)).thenReturn(null);
        when(organizerApplicationMapper.insert(any(OrganizerApplication.class))).thenAnswer(invocation -> {
            OrganizerApplication application = invocation.getArgument(0);
            application.setId(20L);
            return 1;
        });

        var result = organizerApplicationService.apply(
                new OrganizerApplicationRequest("计划组织校园技术分享活动"),
                applicantAuthentication
        );

        assertEquals(OrganizerApplicationStatus.PENDING, result.status());
        assertEquals(20L, result.id());
        verify(operationLogService).record(8L, "ORGANIZER_APPLICATION_SUBMITTED", "ORGANIZER_APPLICATION", 20L);
    }

    @Test
    void rejectsDuplicatePendingApplication() {
        OrganizerApplication application = application(20L, OrganizerApplicationStatus.PENDING);
        when(organizerApplicationMapper.selectByUserId(8L)).thenReturn(application);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> organizerApplicationService.apply(
                        new OrganizerApplicationRequest("重复申请"),
                        applicantAuthentication
                )
        );

        assertEquals(409, exception.getCode());
        verify(organizerApplicationMapper, never()).updateById(any(OrganizerApplication.class));
    }

    @Test
    void administratorApprovalPromotesApplicant() {
        OrganizerApplication application = application(20L, OrganizerApplicationStatus.PENDING);
        when(organizerApplicationMapper.selectByIdForUpdate(20L)).thenReturn(application);
        when(userMapper.selectById(8L)).thenReturn(applicant);

        var result = organizerApplicationService.approve(20L, administratorAuthentication);

        assertEquals(OrganizerApplicationStatus.APPROVED, result.status());
        assertEquals(UserRole.ORGANIZER, applicant.getRole());
        verify(userMapper).updateById(applicant);
        verify(operationLogService).record(1L, "ORGANIZER_APPLICATION_APPROVED", "ORGANIZER_APPLICATION", 20L);
    }

    @Test
    void organizerCannotSubmitAnotherApplication() {
        applicant.setRole(UserRole.ORGANIZER);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> organizerApplicationService.apply(
                        new OrganizerApplicationRequest("再次申请"),
                        applicantAuthentication
                )
        );

        assertEquals(409, exception.getCode());
    }

    private OrganizerApplication application(Long id, OrganizerApplicationStatus status) {
        OrganizerApplication application = new OrganizerApplication();
        application.setId(id);
        application.setUserId(8L);
        application.setReason("计划组织校园技术分享活动");
        application.setStatus(status);
        return application;
    }

    private User user(Long id, String username, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(username);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Authentication authentication(User user) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        return new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities()
        );
    }
}
