package com.message.mesh.controller;

import com.message.mesh.dto.AdminStatsDto;
import com.message.mesh.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatsRestController")
class AdminStatsRestControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminStatsRestController controller;

    @Test
    @DisplayName("stats delegates to the service")
    void statsDelegates() {
        AdminStatsDto expected = mock(AdminStatsDto.class);
        when(adminService.stats()).thenReturn(expected);

        assertThat(controller.stats()).isSameAs(expected);
    }
}
