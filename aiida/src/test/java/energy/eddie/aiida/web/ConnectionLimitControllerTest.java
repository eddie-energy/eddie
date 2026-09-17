// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.dtos.monitoring.MeasurementPointDto;
import energy.eddie.aiida.errors.GlobalExceptionHandler;
import energy.eddie.aiida.services.connectionlimit.ConnectionLimitService;
import energy.eddie.aiida.services.monitoring.MeasurementService;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConnectionLimitController.class)
class ConnectionLimitControllerTest {

    private static final String NOW = "2026-07-10T10:00:00Z";

    @MockitoBean
    private ConnectionLimitService connectionLimitService;

    @MockitoBean
    private MeasurementService measurementService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest
    @MethodSource("filterArguments")
    @WithMockUser
    void givenValidFilters_callServiceWithFiltersAndReturnsLimit(
            @Nullable String providedFrom,
            @Nullable String providedTo,
            String expectedFrom,
            String expectedTo
    ) throws Exception {
        var pid = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
        var mid = "003114735";
        var did = "5dc71d7e-e8cd-4403-a3a8-d3c095c97a12";
        var start = Instant.parse("2026-07-10T08:45:00Z");
        var end = Instant.parse("2026-07-10T09:00:00Z");
        var min = new BigDecimal("3.0");
        var max = new BigDecimal("8.0");

        var limits = List.of(new ConnectionLimitDto(pid, mid, did, start, end, min, max));
        when(connectionLimitService.getConnectionLimits(any(), any(), any(), any())).thenReturn(limits);

        var request = get("/connection-limits").param("permissionId", pid.toString()).param("meterId", mid);

        if (providedFrom != null) {
            request.param("from", providedFrom);
        }

        if (providedTo != null) {
            request.param("to", providedTo);
        }

        mockMvc.perform(request)
               .andExpect(status().isOk())
               .andExpect(content().json(objectMapper.writeValueAsString(limits)));

        verify(connectionLimitService).getConnectionLimits(pid,
                                                           mid,
                                                           Instant.parse(expectedFrom),
                                                           Instant.parse(expectedTo));
    }

    @Test
    @WithMockUser
    void givenInvalidInstantOrDuration_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/connection-limits").param("from", "foo"))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void givenMonitorablePermission_returnsMeasurementPoints() throws Exception {
        var pid = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
        var timestamp = Instant.parse("2026-07-10T09:55:00Z");
        var points = List.of(new MeasurementPointDto(timestamp, new BigDecimal("8.5"), new BigDecimal("8.5")));
        when(measurementService.getMeasurements(any(), any(), any())).thenReturn(points);

        mockMvc.perform(get("/connection-limits/{permissionId}/measurements", pid)
                                .param("from", "2026-07-09T10:00:00Z")
                                .param("to", "2026-07-10T10:00:00Z"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(1))
               .andExpect(jsonPath("$[0].timestamp").value("2026-07-10T09:55:00Z"))
               .andExpect(jsonPath("$[0].minPowerKw").value(8.5))
               .andExpect(jsonPath("$[0].maxPowerKw").value(8.5));

        verify(measurementService).getMeasurements(pid,
                                                   Instant.parse("2026-07-09T10:00:00Z"),
                                                   Instant.parse("2026-07-10T10:00:00Z"));
    }

    @Test
    @WithMockUser
    void givenNoMeasurementTimeFrame_queriesTheLastDay() throws Exception {
        var pid = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");
        when(measurementService.getMeasurements(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/connection-limits/{permissionId}/measurements", pid))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$").isEmpty());

        verify(measurementService).getMeasurements(pid,
                                                   Instant.parse(NOW).minus(Duration.ofDays(1)),
                                                   Instant.parse(NOW));
    }

    @Test
    @WithMockUser
    void givenInvalidMeasurementTimeFrame_returnsBadRequest() throws Exception {
        var pid = UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3");

        mockMvc.perform(get("/connection-limits/{permissionId}/measurements", pid).param("from", "foo"))
               .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> filterArguments() {
        var from = "2026-07-10T00:00:00Z";
        var to = "2026-07-10T23:59:59Z";
        return Stream.of(Arguments.of(from, to, from, to),
                         Arguments.of(null, to, NOW, to),
                         Arguments.of(from, null, from, NOW),
                         Arguments.of(null, null, NOW, NOW),
                         Arguments.of("", "  ", NOW, NOW),
                         Arguments.of("-P1D", "P1D", "2026-07-09T10:00:00Z", "2026-07-11T10:00:00Z"));
    }

    @TestConfiguration
    static class ConnectionLimitControllerTestConfiguration {
        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Bean
        public Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-10T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
