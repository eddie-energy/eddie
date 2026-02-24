// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.CimTestConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrCreatedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrMalformedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrValidatedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequestBuilder;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Import(CimTestConfiguration.class)
class PermissionRequestServiceTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private PermissionRequestService permissionRequestService;
    @MockitoBean
    private FrPermissionRequestRepository repository;
    @SuppressWarnings("unused")
    @MockitoBean
    private HistoricalDataService historicalDataService;
    @MockitoBean
    private Outbox outbox;
    @MockitoBean
    private DataNeedCalculationService<DataNeed> calculationService;
    @MockitoBean
    @SuppressWarnings("unused")
    private DataNeedsService dataNeedsService;

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        var now = LocalDate.now(ZONE_ID_FR);
        when(calculationService.calculate("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeedResult(List.of(Granularity.P1D),
                                                                      new Timeframe(now, now.plusDays(10)),
                                                                      new Timeframe(now, now.plusDays(10))));

        // When
        permissionRequestService.createPermissionRequest(request);

        // Then
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrValidatedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_createsPermissionRequest_forAccountingPointDataNeed() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        var now = LocalDate.now(ZONE_ID_FR);
        when(calculationService.calculate("dnid"))
                .thenReturn(new AccountingPointDataNeedResult(new Timeframe(now, now)));

        // When
        permissionRequestService.createPermissionRequest(request);

        // Then
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrValidatedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnAiidaDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        var timeframe = new Timeframe(LocalDate.now(), LocalDate.now());
        when(calculationService.calculate("dnid"))
                .thenReturn(new AiidaDataNeedResult(Set.of(), Set.of(), timeframe));
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(request));
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnInvalidDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotSupportedResult(""));
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(request));
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnUnknownDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        when(calculationService.calculate("dnid"))
                .thenReturn(new DataNeedNotFoundResult());
        // When
        // Then
        assertThrows(DataNeedNotFoundException.class,
                     () -> permissionRequestService.createPermissionRequest(request));
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrMalformedEvent.class));
    }

    @Test
    void testAuthorizePermissionRequest_acceptsPermissionRequest() throws PermissionNotFoundException {
        // Given
        EnedisPermissionRequest permissionRequest = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setStart(LocalDate.now(ZoneOffset.UTC).minusDays(3))
                .setEnd(LocalDate.now(ZoneOffset.UTC))
                .setGranularity(Granularity.P1D)
                .create();
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(permissionRequest));

        // When
        permissionRequestService.authorizePermissionRequest("pid", new String[]{"upid"});

        // Then
        verify(outbox).commit(argThat(event -> event.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        verify(outbox).commit(isA(FrAcceptedEvent.class));
    }

    @Test
    @SuppressWarnings("java:S5961")
    void testAuthorizePermissionRequest_withMultipleUsagePointIds_acceptsFirstAndCreatesNewOnes() throws PermissionNotFoundException {
        // Given
        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        ArgumentCaptor<FrCreatedEvent> createdCaptor = ArgumentCaptor.forClass(FrCreatedEvent.class);
        ArgumentCaptor<FrValidatedEvent> validatedCaptor = ArgumentCaptor.forClass(FrValidatedEvent.class);
        ArgumentCaptor<FrAcceptedEvent> acceptedCaptor = ArgumentCaptor.forClass(FrAcceptedEvent.class);

        EnedisPermissionRequest permissionRequest = new EnedisPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setStart(LocalDate.now(ZoneOffset.UTC).minusDays(3))
                .setEnd(LocalDate.now(ZoneOffset.UTC))
                .setGranularity(Granularity.P1D)
                .create();
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(permissionRequest));

        // When
        permissionRequestService.authorizePermissionRequest("pid", new String[]{"upid", "upid2", "upid3"});

        // Then
        verify(outbox, times(10)).commit(eventCaptor.capture());
        verify(outbox,
               times(3)).commit(argThat(event -> event.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        verify(outbox, times(3)).commit(acceptedCaptor.capture());
        verify(outbox, times(2)).commit(createdCaptor.capture());
        verify(outbox, times(2)).commit(validatedCaptor.capture());

        assertAll(
                // verify that the events are emitted in the correct order
                () -> assertEquals(10, eventCaptor.getAllValues().size()),
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                   eventCaptor.getAllValues().getFirst().status()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, eventCaptor.getAllValues().get(1).status()),
                () -> assertEquals(PermissionProcessStatus.CREATED, eventCaptor.getAllValues().get(2).status()),
                () -> assertEquals(PermissionProcessStatus.VALIDATED, eventCaptor.getAllValues().get(3).status()),
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                   eventCaptor.getAllValues().get(4).status()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, eventCaptor.getAllValues().get(5).status()),
                () -> assertEquals(PermissionProcessStatus.CREATED, eventCaptor.getAllValues().get(6).status()),
                () -> assertEquals(PermissionProcessStatus.VALIDATED, eventCaptor.getAllValues().get(7).status()),
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                   eventCaptor.getAllValues().get(8).status()),
                () -> assertEquals(PermissionProcessStatus.ACCEPTED, eventCaptor.getAllValues().get(9).status()),
                // verify that the correct usage point ids are used
                () -> assertEquals("upid", acceptedCaptor.getAllValues().getFirst().usagePointId()),
                () -> assertEquals("upid2", acceptedCaptor.getAllValues().get(1).usagePointId()),
                () -> assertEquals("upid3", acceptedCaptor.getAllValues().getLast().usagePointId()),
                // verify that the correct permission ids are used
                () -> assertNotEquals("pid", createdCaptor.getAllValues().getFirst().permissionId()),
                () -> assertNotEquals(createdCaptor.getAllValues().getFirst().permissionId(),
                                      createdCaptor.getAllValues().getLast().permissionId()),
                () -> assertEquals(createdCaptor.getAllValues().getFirst().permissionId(),
                                   validatedCaptor.getAllValues().getFirst().permissionId()),
                () -> assertEquals(createdCaptor.getAllValues().getFirst().permissionId(),
                                   acceptedCaptor.getAllValues().get(1).permissionId()),
                () -> assertEquals(createdCaptor.getAllValues().getLast().permissionId(),
                                   validatedCaptor.getAllValues().getLast().permissionId()),
                () -> assertEquals(createdCaptor.getAllValues().getLast().permissionId(),
                                   acceptedCaptor.getAllValues().get(2).permissionId())

        );
    }

    @Test
    void testAuthorizePermissionRequest_withNonExistingPermissionRequest_throws() {
        // Given, When, Then
        assertThrows(PermissionNotFoundException.class,
                     () -> permissionRequestService.authorizePermissionRequest("NonExistingPid", new String[]{"upid"}));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TimeoutConfiguration timeoutConfiguration() {
            return new TimeoutConfiguration(24);
        }
    }
}
