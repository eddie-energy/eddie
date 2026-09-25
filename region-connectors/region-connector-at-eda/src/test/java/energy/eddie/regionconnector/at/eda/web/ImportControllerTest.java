// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.web;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotFoundResult;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestToImport;
import energy.eddie.regionconnector.at.eda.services.ImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ImportController.class, properties = "eddie.management.server.urlprefix=management")
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class ImportControllerTest {
    @MockitoBean
    private ImportService importService;
    @MockitoBean
    private DataNeedCalculationService calculationService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenPermissionRequestToImport_whenImportEndpointIsCalled_returns201Created() throws Exception {
        // Given
        var pr = new PermissionRequestToImport("cid",
                                               "AT0000000000000000000000000000000",
                                               "dnid",
                                               "AT001000",
                                               "Consent-ID",
                                               ZonedDateTime.parse("2026-01-01T00:00:00Z"));
        var json = new ObjectMapper().writeValueAsString(pr);
        when(importService.importPermissionRequest(pr)).thenReturn(new CreatedPermissionRequest(List.of("pid")));
        when(calculationService.calculate(anyString(), any())).thenReturn(new DataNeedNotFoundResult());

        // Then
        mockMvc.perform(post("/management/permission-request/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
               .andDo(MockMvcResultHandlers.print())
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.permissionIds[0]").value("pid"))
               .andExpect(header().string("Location", "/api/connection-status-messages?permission-id=pid"));
    }

    @Test
    void importPermissionRequest_400WhenOnlyOneMeterReadingDateIsPresent() throws Exception {
        var request = new PermissionRequestToImport(
                "cid",
                "AT0000000000000000000000000000000",
                "dnid",
                "AT001000",
                "Consent-ID",
                ZonedDateTime.parse("2026-01-01T00:00:00Z"),
                ZonedDateTime.parse("2026-01-01T00:00:00Z"),
                null
        );

        mockMvc.perform(post("/management/permission-request/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(importService);
    }

    @Test
    void importPermissionRequest_400WhenMeterReadingStartIsAfterEnd() throws Exception {
        var request = new PermissionRequestToImport(
                "cid",
                "AT0000000000000000000000000000000",
                "dnid",
                "AT001000",
                "Consent-ID",
                ZonedDateTime.parse("2026-01-01T00:00:00Z"),
                ZonedDateTime.parse("2026-01-02T00:00:00Z"),
                ZonedDateTime.parse("2026-01-01T00:00:00Z")
        );

        mockMvc.perform(post("/management/permission-request/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(importService);
    }

    @Test
    void importPermissionRequest_400WhenMeterReadingDatesAreOutsideDataNeedTimeframe() throws Exception {
        when(calculationService.calculate(anyString(), any())).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(PT15M),
                        new Timeframe(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3)),
                        new Timeframe(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3)),
                        null
                )
        );
        var request = new PermissionRequestToImport(
                "cid",
                "AT0000000000000000000000000000000",
                "dnid",
                "AT001000",
                "Consent-ID",
                ZonedDateTime.parse("2026-01-01T00:00:00Z"),
                ZonedDateTime.parse("2026-01-01T00:00:00Z"),
                ZonedDateTime.parse("2026-01-02T00:00:00Z")
        );

        mockMvc.perform(post("/management/permission-request/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(importService);
    }

    @Test
    void importPermissionRequest_400WhenMeterReadingEndIsAfterDataNeedTimeframe() throws Exception {
        when(calculationService.calculate(anyString(), any())).thenReturn(
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(PT15M),
                        new Timeframe(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3)),
                        new Timeframe(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3)),
                        null
                )
        );
        var request = new PermissionRequestToImport(
                "cid",
                "AT0000000000000000000000000000000",
                "dnid",
                "AT001000",
                "Consent-ID",
                ZonedDateTime.parse("2026-01-01T00:00:00Z"),
                ZonedDateTime.parse("2026-01-02T00:00:00Z"),
                ZonedDateTime.parse("2026-01-04T00:00:00Z")
        );

        mockMvc.perform(post("/management/permission-request/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(importService);
    }

    @Test
    void importPermissionRequest_400WhenCreationDateTimeIsInTheFuture() throws Exception {
        var request = new PermissionRequestToImport(
                "cid",
                "AT0000000000000000000000000000000",
                "dnid",
                "AT001000",
                "Consent-ID",
                ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(1)
        );

        mockMvc.perform(post("/management/permission-request/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(importService);
    }
}
