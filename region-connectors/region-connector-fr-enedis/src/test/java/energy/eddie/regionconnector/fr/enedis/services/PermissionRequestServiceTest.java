package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrCreatedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrMalformedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrValidatedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.timeout.TimeoutConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class PermissionRequestServiceTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private PermissionRequestService permissionRequestService;
    @MockBean
    private FrPermissionRequestRepository repository;
    @SuppressWarnings("unused")
    @MockBean
    private HistoricalDataService historicalDataService;
    @MockBean
    private DataNeedsService dataNeedsService;
    @Mock
    private ValidatedHistoricalDataDataNeed mockVhdDataNeed;
    @Mock
    private AiidaDataNeed unsupportedDataNeed;
    @MockBean
    private Outbox outbox;
    @MockBean
    private DataNeedCalculationService<DataNeed> calculationService;

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        var now = LocalDate.now(ZONE_ID_FR);
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(mockVhdDataNeed));
        when(calculationService.calculate(mockVhdDataNeed))
                .thenReturn(new DataNeedCalculation(true,
                                                    List.of(Granularity.P1D),
                                                    new Timeframe(now, now.plusDays(10)),
                                                    new Timeframe(now, now.plusDays(10))));

        // When
        permissionRequestService.createPermissionRequest(request);

        // Then
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrValidatedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnInvalidDataNeed() {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(unsupportedDataNeed));
        when(calculationService.calculate(unsupportedDataNeed))
                .thenReturn(new DataNeedCalculation(false));
        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(request));
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrMalformedEvent.class));
    }

    @Test
    void testCreatePermissionRequest_emitsMalformedOnInvalidTimeframe() {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(mockVhdDataNeed));
        when(calculationService.calculate(mockVhdDataNeed))
                .thenReturn(new DataNeedCalculation(true,
                                                    List.of(Granularity.P1D),
                                                    null,
                                                    null));

        // When
        // Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(request));
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrMalformedEvent.class));
    }

    @Test
    void testAuthorizePermissionRequest_acceptsPermissionRequest() throws PermissionNotFoundException {
        // Given
        EnedisPermissionRequest permissionRequest = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                LocalDate.now(ZoneOffset.UTC).minusDays(3),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.P1D,
                PermissionProcessStatus.VALIDATED,
                null,
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                UsagePointType.CONSUMPTION
        );
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(permissionRequest));

        // When
        permissionRequestService.authorizePermissionRequest("pid", new String[]{"upid"});

        // Then
        verify(outbox).commit(argThat(event -> event.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        verify(outbox).commit(isA(FrAcceptedEvent.class));
    }

    @Test
    void testAuthorizePermissionRequest_withMultipleUsagePointIds_acceptsFirstAndCreatesNewOnes() throws PermissionNotFoundException {
        // Given
        ArgumentCaptor<PermissionEvent> eventCaptor = ArgumentCaptor.forClass(PermissionEvent.class);
        ArgumentCaptor<FrCreatedEvent> createdCaptor = ArgumentCaptor.forClass(FrCreatedEvent.class);
        ArgumentCaptor<FrValidatedEvent> validatedCaptor = ArgumentCaptor.forClass(FrValidatedEvent.class);
        ArgumentCaptor<FrAcceptedEvent> acceptedCaptor = ArgumentCaptor.forClass(FrAcceptedEvent.class);

        EnedisPermissionRequest permissionRequest = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                LocalDate.now(ZoneOffset.UTC).minusDays(3),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.P1D,
                PermissionProcessStatus.VALIDATED,
                null,
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                UsagePointType.CONSUMPTION
        );
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

    @Test
    @DirtiesContext
    void testFindConnectionStatusById_returnsConnectionStatus() {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC).minusDays(3);
        var end = LocalDate.now(ZoneOffset.UTC);
        var request = new EnedisPermissionRequest("pid",
                                                  "cid",
                                                  "dnid",
                                                  start,
                                                  end,
                                                  Granularity.P1D,
                                                  PermissionProcessStatus.VALIDATED,
                                                  null,
                                                  null,
                                                  ZonedDateTime.now(ZoneOffset.UTC),
                                                  UsagePointType.CONSUMPTION);
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(request));

        // When
        var res = permissionRequestService.findConnectionStatusMessageById("pid");

        // Then
        assertTrue(res.isPresent());
        assertEquals("pid", res.get().permissionId());
    }

    @Test
    @DirtiesContext
    void testFindConnectionStatusById_withNotExistingPermissionId_returnsEmpty() {
        // Given
        var start = LocalDate.now(ZoneOffset.UTC).minusDays(3);
        var end = LocalDate.now(ZoneOffset.UTC);
        var request = new EnedisPermissionRequest("pid",
                                                  "cid",
                                                  "dnid",
                                                  start,
                                                  end,
                                                  Granularity.P1D,
                                                  PermissionProcessStatus.VALIDATED,
                                                  null,
                                                  null,
                                                  ZonedDateTime.now(ZoneOffset.UTC),
                                                  UsagePointType.CONSUMPTION);
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(request));

        // When
        var res = permissionRequestService.findConnectionStatusMessageById("asdfasdfadsf");

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    void givenUnsupportedGranularity_throws() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        when(dataNeedsService.findById(any())).thenReturn(Optional.of(mockVhdDataNeed));
        when(calculationService.calculate(mockVhdDataNeed))
                .thenReturn(new DataNeedCalculation(true,
                                                    List.of(),
                                                    new Timeframe(now, now.plusDays(10)),
                                                    new Timeframe(now, now.plusDays(10))));
        PermissionRequestForCreation create = new PermissionRequestForCreation("foo", "bar");

        // When, Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(create));
        verify(outbox).commit(isA(FrCreatedEvent.class));
        verify(outbox).commit(isA(FrMalformedEvent.class));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TimeoutConfiguration timeoutConfiguration() {
            return new TimeoutConfiguration(24);
        }
    }
}
