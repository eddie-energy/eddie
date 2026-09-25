// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionlimit.UpdateConnectionLimitMonitoringDto;
import energy.eddie.aiida.errors.GlobalExceptionHandler;
import energy.eddie.aiida.errors.connectionlimit.ConnectionLimitMonitoringNotAllowedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitMonitoring;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitMonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectionLimitMonitoringController.class)
class ConnectionLimitMonitoringControllerTest {
    private static final UUID PERMISSION_ID = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("51d0a13e-688a-454d-acab-7a6b2951cde2");

    @MockitoBean
    private ConnectionLimitMonitoringService connectionLimitMonitoringService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void givenDataSource_assignsMonitoring() throws Exception {
        when(connectionLimitMonitoringService.updateConnectionLimitMonitoring(eq(PERMISSION_ID), eq(DATA_SOURCE_ID)))
                .thenReturn(new ConnectionLimitMonitoring(PERMISSION_ID, DATA_SOURCE_ID));

        var content = objectMapper.writeValueAsString(new UpdateConnectionLimitMonitoringDto(DATA_SOURCE_ID));
        mockMvc.perform(put("/connection-limit-monitoring/{permissionId}", PERMISSION_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.permissionId").value(PERMISSION_ID.toString()))
               .andExpect(jsonPath("$.dataSourceId").value(DATA_SOURCE_ID.toString()));
    }

    @Test
    @WithMockUser
    void givenMissingDataSourceId_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/connection-limit-monitoring/{permissionId}", PERMISSION_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void givenMissingDataSource_returnsNotFound() throws Exception {
        when(connectionLimitMonitoringService.updateConnectionLimitMonitoring(any(), any()))
                .thenThrow(new DataSourceNotFoundException(DATA_SOURCE_ID));

        var content = objectMapper.writeValueAsString(new UpdateConnectionLimitMonitoringDto(DATA_SOURCE_ID));
        mockMvc.perform(put("/connection-limit-monitoring/{permissionId}", PERMISSION_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(content))
               .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void givenNotAllowed_returnsBadRequest() throws Exception {
        when(connectionLimitMonitoringService.updateConnectionLimitMonitoring(any(), any())).thenThrow(
                new ConnectionLimitMonitoringNotAllowedException(PERMISSION_ID));

        mockMvc.perform(put("/connection-limit-monitoring/{permissionId}", PERMISSION_ID)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new UpdateConnectionLimitMonitoringDto(
                                        DATA_SOURCE_ID))))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void givenAssignment_deletesMonitoring() throws Exception {
        mockMvc.perform(delete("/connection-limit-monitoring/{permissionId}", PERMISSION_ID).with(csrf()))
               .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void givenMissingPermissionOnDelete_returnsNotFound() throws Exception {
        doThrow(new PermissionNotFoundException(PERMISSION_ID)).when(connectionLimitMonitoringService)
                                                               .deleteConnectionLimitMonitoring(PERMISSION_ID);

        mockMvc.perform(delete("/connection-limit-monitoring/{permissionId}", PERMISSION_ID).with(csrf()))
               .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class ConnectionLimitMonitoringControllerTestConfiguration {
        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }
}
