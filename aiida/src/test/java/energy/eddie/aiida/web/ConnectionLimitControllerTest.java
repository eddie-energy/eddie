// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.errors.GlobalExceptionHandler;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectionLimitController.class)
class ConnectionLimitControllerTest {

    @MockitoBean
    private ConnectionLimitService connectionLimitService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void givenValidFilters_callServiceWithFiltersAndReturnsLimit() throws Exception {
        var pid = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
        var mid = "003114735";
        var start = Instant.parse("2026-07-10T08:45:00Z");
        var end = Instant.parse("2026-07-10T09:00:00Z");
        var min = new BigDecimal("3.0");
        var max = new BigDecimal("8.0");
        var from = Instant.parse("2026-07-10T00:00:00Z");
        var to = Instant.parse("2026-07-10T23:59:59Z");
        var offset = 1;

        var limits = List.of(new ConnectionLimitDto(pid, mid, start, end, min, max));
        when(connectionLimitService.getConnectionLimits(any(), any(), any(), any(), any())).thenReturn(limits);

        mockMvc.perform(get("/connection-limits")
                                .param("permissionId", pid.toString())
                                .param("meterId", mid)
                                .param("from", from.toString())
                                .param("to", to.toString())
                                .param("offset", Integer.toString(offset)))
               .andExpect(status().isOk())
               .andExpect(content().json(objectMapper.writeValueAsString(limits)));

        verify(connectionLimitService).getConnectionLimits(pid, mid, from, to, offset);
    }

    @TestConfiguration
    static class ConnectionLimitControllerTestConfiguration {
        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }
}
