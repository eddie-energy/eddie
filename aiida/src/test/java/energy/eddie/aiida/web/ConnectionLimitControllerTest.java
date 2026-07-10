// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.errors.GlobalExceptionHandler;
import energy.eddie.aiida.errors.connectionlimit.PermissionDoesNotSupportConnectionLimitsException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_JSON_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectionLimitController.class)
class ConnectionLimitControllerTest {
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");

    @MockitoBean
    private ConnectionLimitService connectionLimitService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void givenValidRequest_returnsLimits() throws Exception {
        when(connectionLimitService.getConnectionLimits(any(), any(), any(), any())).thenReturn(
                List.of(new ConnectionLimitDto(
                        PERMISSION_ID,
                        "003114735",
                        Instant.parse("2026-07-10T08:45:00Z"),
                        Instant.parse("2026-07-10T09:00:00Z"),
                        new BigDecimal("3.0"),
                        new BigDecimal("8.0")
                ))
        );

        mockMvc.perform(get("/connection-limits/{permissionId}", PERMISSION_ID))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1))
               .andExpect(jsonPath("$[0].permissionId").value(PERMISSION_ID.toString()))
               .andExpect(jsonPath("$[0].meterId").value("003114735"));
    }

    @Test
    @WithMockUser
    void givenUnknownPermission_returnsNotFound() throws Exception {
        when(connectionLimitService.getConnectionLimits(any(), any(), any(), any())).thenThrow(
                new PermissionNotFoundException(PERMISSION_ID)
        );

        mockMvc.perform(get("/connection-limits/{permissionId}", PERMISSION_ID))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message", containsString(PERMISSION_ID.toString())));
    }

    @Test
    @WithMockUser
    void givenPermissionWithoutMinMaxSchema_returnsUnprocessableContent() throws Exception {
        when(connectionLimitService.getConnectionLimits(any(), any(), any(), any())).thenThrow(
                new PermissionDoesNotSupportConnectionLimitsException(PERMISSION_ID)
        );

        mockMvc.perform(get("/connection-limits/{permissionId}", PERMISSION_ID))
               .andExpect(status().isUnprocessableContent())
               .andExpect(jsonPath(ERRORS_JSON_PATH + "[0].message",
                                   containsString("does not support connection limits")));
    }

    @Test
    @WithMockUser
    void givenNegativeOffset_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/connection-limits/{permissionId}", PERMISSION_ID).param("offset", "-1"))
               .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class ConnectionLimitControllerTestConfiguration {
        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }
}
