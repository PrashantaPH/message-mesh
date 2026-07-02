package com.message.mesh.controller;

import com.message.mesh.dto.AdminUserDetailDto;
import com.message.mesh.dto.AdminUserDto;
import com.message.mesh.dto.CreateUserRequest;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.dto.ResetPasswordRequest;
import com.message.mesh.dto.UpdateRoleRequest;
import com.message.mesh.dto.UpdateStatusRequest;
import com.message.mesh.enums.UserRole;
import com.message.mesh.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminRestController")
class AdminRestControllerTest {

    @Mock
    private AdminService adminService;
    @Mock
    private Principal principal;

    @InjectMocks
    private AdminRestController controller;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(principal.getName()).thenReturn("root");
    }

    @Test
    @DisplayName("list clamps a negative page and an oversized page size")
    void listClampsPagination() {
        @SuppressWarnings("unchecked")
        PagedResponse<AdminUserDto> expected = mock(PagedResponse.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(adminService.listUsers(eq("q"), eq(UserRole.USER), eq(true), pageable.capture()))
                .thenReturn(expected);

        PagedResponse<AdminUserDto> result = controller.list("q", UserRole.USER, true, -5, 500);

        assertThat(result).isSameAs(expected);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("list raises a zero page size to the minimum of one")
    void listRaisesZeroSize() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(adminService.listUsers(isNull(), isNull(), isNull(), pageable.capture()))
                .thenReturn(mock(PagedResponse.class));

        controller.list(null, null, null, 2, 0);

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("create responds 201 Created")
    void createReturnsCreated() {
        CreateUserRequest req = new CreateUserRequest("bob", "secret1", "Bob", UserRole.USER);
        AdminUserDto created = mock(AdminUserDto.class);
        when(adminService.createUser(req, "root")).thenReturn(created);

        ResponseEntity<AdminUserDto> response = controller.create(req, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);
    }

    @Test
    @DisplayName("detail delegates to getUserDetail")
    void detailDelegates() {
        AdminUserDetailDto expected = mock(AdminUserDetailDto.class);
        when(adminService.getUserDetail(userId)).thenReturn(expected);

        assertThat(controller.detail(userId)).isSameAs(expected);
    }

    @Test
    @DisplayName("updateRole delegates to the service")
    void updateRoleDelegates() {
        AdminUserDto expected = mock(AdminUserDto.class);
        when(adminService.updateRole(userId, UserRole.ADMIN, "root")).thenReturn(expected);

        assertThat(controller.updateRole(userId, new UpdateRoleRequest(UserRole.ADMIN), principal))
                .isSameAs(expected);
    }

    @Test
    @DisplayName("updateStatus delegates to the service")
    void updateStatusDelegates() {
        AdminUserDto expected = mock(AdminUserDto.class);
        when(adminService.updateStatus(userId, false, "root")).thenReturn(expected);

        assertThat(controller.updateStatus(userId, new UpdateStatusRequest(false), principal))
                .isSameAs(expected);
    }

    @Test
    @DisplayName("resetPassword returns 204 and delegates to the service")
    void resetPasswordReturnsNoContent() {
        ResponseEntity<Void> response =
                controller.resetPassword(userId, new ResetPasswordRequest("new-secret"), principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(adminService).resetPassword(userId, "new-secret", "root");
    }

    @Test
    @DisplayName("delete returns 204 and delegates to the service")
    void deleteReturnsNoContent() {
        ResponseEntity<Void> response = controller.delete(userId, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(adminService).deleteUser(userId, "root");
    }

    @Test
    @DisplayName("revokeSessions delegates to the service")
    void revokeSessionsDelegates() {
        AdminUserDto expected = mock(AdminUserDto.class);
        when(adminService.revokeSessions(userId, "root")).thenReturn(expected);

        assertThat(controller.revokeSessions(userId, principal)).isSameAs(expected);
    }
}
