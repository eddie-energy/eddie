// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.cds.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.cds.exceptions.UnknownPermissionAdministratorException;
import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.events.CreatedEvent;
import energy.eddie.regionconnector.cds.permission.events.MalformedEvent;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import energy.eddie.regionconnector.cds.services.oauth.AuthorizationService;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestCreationServiceTest {
    @Mock
    private CdsServerRepository repository;
    @Mock
    private Outbox outbox;
    @Mock
    private CdsServerCalculationService calculationService;
    @Mock
    private AuthorizationService authorizationService;
    @InjectMocks
    private PermissionRequestCreationService service;
    @Captor
    private ArgumentCaptor<PermissionEvent> eventCaptor;

    @Test
    void testCreatePermissionRequest_withUnknownCDSServer_throws() {
        // Given
        var creation = new PermissionRequestForCreation(0L, "dnid", "cid");

        // When & Then
        var ex = assertThrows(UnknownPermissionAdministratorException.class,
                              () -> service.createPermissionRequest(creation));
        assertEquals("Unknown permission administrator: 0", ex.getMessage());
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var malformedEvent = assertInstanceOf(MalformedEvent.class, eventCaptor.getValue());
        assertEquals(List.of(new AttributeError("cdsId", "Unknown permission administrator")), malformedEvent.errors());
        var createdEvent = assertInstanceOf(CreatedEvent.class, eventCaptor.getAllValues().getFirst());
        assertAll(
                () -> assertEquals("0", createdEvent.getDataSourceInformation().permissionAdministratorId()),
                () -> assertEquals("cid", createdEvent.connectionId()),
                () -> assertEquals("dnid", createdEvent.dataNeedId())
        );
    }

    @Test
    void testCreatePermissionRequest_withUnknownDataNeed_throws() {
        // Given
        var cdsServer = getCdsServer();
        when(repository.findById(0L)).thenReturn(Optional.of(cdsServer));
        when(calculationService.calculate(eq("dnid"), eq(cdsServer), any()))
                .thenReturn(new DataNeedNotFoundResult());
        var creation = new PermissionRequestForCreation(0L, "dnid", "cid");

        // When & Then
        assertThrows(DataNeedNotFoundException.class, () -> service.createPermissionRequest(creation));
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var malformedEvent = assertInstanceOf(MalformedEvent.class, eventCaptor.getValue());
        assertEquals(List.of(new AttributeError("dataNeedId", "Data need not found")), malformedEvent.errors());
    }

    @Test
    void testCreatePermissionRequest_withAccountingPointDataNeed_emitsValidated() throws DataNeedNotFoundException, UnknownPermissionAdministratorException, UnsupportedDataNeedException {
        // Given
        var cdsServer = getCdsServer();
        when(repository.findById(0L)).thenReturn(Optional.of(cdsServer));
        var today = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(today, today);
        when(calculationService.calculate(eq("dnid"), eq(cdsServer), any()))
                .thenReturn(new AccountingPointDataNeedResult(timeframe));
        var uri = URI.create("urn:example:bwc4JK-ESC0w8acc191e-Y1LTC2");
        when(authorizationService.createOAuthRequest(eq(cdsServer), anyString()))
                .thenReturn(uri);
        var creation = new PermissionRequestForCreation(0L, "dnid", "cid");

        // When
        var res = service.createPermissionRequest(creation);

        // Then
        assertAll(
                () -> assertEquals(uri, res.redirectUri()),
                () -> assertNotNull(res.permissionId())
        );
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var event = assertInstanceOf(SimpleEvent.class, eventCaptor.getAllValues().get(1));
        assertThat(event)
                .isInstanceOf(SimpleEvent.class)
                .extracting(SimpleEvent::status)
                .isEqualTo(PermissionProcessStatus.VALIDATED);
    }

    @Test
    void testCreatePermissionRequest_withAiidaDataNeed_throws() {
        // Given
        var cdsServer = getCdsServer();
        when(repository.findById(0L)).thenReturn(Optional.of(cdsServer));
        when(calculationService.calculate(eq("dnid"), eq(cdsServer), any()))
                .thenReturn(new AiidaDataNeedResult(Set.of(),
                                                    Set.of(),
                                                    new Timeframe(LocalDate.now(ZoneOffset.UTC),
                                                                  LocalDate.now(ZoneOffset.UTC))));
        var creation = new PermissionRequestForCreation(0L, "dnid", "cid");

        // When & Then
        assertThrows(UnsupportedDataNeedException.class, () -> service.createPermissionRequest(creation));
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var malformedEvent = assertInstanceOf(MalformedEvent.class, eventCaptor.getValue());
        assertEquals(List.of(new AttributeError("dataNeedId", "AiidaDataNeedResult not supported!")),
                     malformedEvent.errors());
    }

    @Test
    void testCreatePermissionRequest_withUnsupportedDataNeed_throws() {
        // Given
        var cdsServer = getCdsServer();
        when(repository.findById(0L)).thenReturn(Optional.of(cdsServer));
        when(calculationService.calculate(eq("dnid"), eq(cdsServer), any()))
                .thenReturn(new DataNeedNotSupportedResult("Not supported"));
        var creation = new PermissionRequestForCreation(0L, "dnid", "cid");

        // When & Then
        assertThrows(UnsupportedDataNeedException.class, () -> service.createPermissionRequest(creation));
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var malformedEvent = assertInstanceOf(MalformedEvent.class, eventCaptor.getValue());
        assertEquals(List.of(new AttributeError("dataNeedId", "Not supported")), malformedEvent.errors());
    }

    @Test
    void testCreatePermissionRequest_withValidRequest_emitsValidatedEvent() throws DataNeedNotFoundException, UnknownPermissionAdministratorException, UnsupportedDataNeedException {
        // Given
        var cdsServer = getCdsServer();
        when(repository.findById(0L)).thenReturn(Optional.of(cdsServer));
        var today = LocalDate.now(ZoneOffset.UTC);
        var timeframe = new Timeframe(today, today);
        when(calculationService.calculate(eq("dnid"), eq(cdsServer), any()))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.PT15M),
                                                                      timeframe,
                                                                      timeframe));
        var uri = URI.create("urn:example:bwc4JK-ESC0w8acc191e-Y1LTC2");
        when(authorizationService.createOAuthRequest(eq(cdsServer), anyString()))
                .thenReturn(uri);
        var creation = new PermissionRequestForCreation(0L, "dnid", "cid");

        // When
        var res = service.createPermissionRequest(creation);

        // Then
        assertAll(
                () -> assertEquals(uri, res.redirectUri()),
                () -> assertNotNull(res.permissionId())
        );
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var event = assertInstanceOf(ValidatedEvent.class, eventCaptor.getAllValues().get(1));
        assertAll(
                () -> assertEquals(Granularity.PT15M, event.granularity()),
                () -> assertEquals(today, event.start()),
                () -> assertEquals(today, event.end())
        );
    }

    private static CdsServer getCdsServer() {
        return new CdsServerBuilder().setBaseUri("http://localhost")
                                     .setAdminClientId("client-id")
                                     .setAdminClientSecret("client-secret")
                                     .build();
    }
}