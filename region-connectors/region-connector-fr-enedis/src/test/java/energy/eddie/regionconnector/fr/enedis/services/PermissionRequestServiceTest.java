package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrAcceptedEvent;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    @MockBean
    private HistoricalDataService historicalDataService;
    @MockBean
    private DataNeedsService dataNeedsService;
    @Mock
    private ValidatedHistoricalDataDataNeed mockVhdDataNeed;
    @Mock
    private AbsoluteDuration absoluteDuration;
    @MockBean
    private Outbox outbox;

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        when(dataNeedsService.findById("dnid")).thenReturn(Optional.of(mockVhdDataNeed));
        when(mockVhdDataNeed.duration()).thenReturn(absoluteDuration);
        when(absoluteDuration.start()).thenReturn(LocalDate.now(ZONE_ID_FR));
        when(absoluteDuration.end()).thenReturn(LocalDate.now(ZONE_ID_FR).plusDays(10));
        when(mockVhdDataNeed.minGranularity()).thenReturn(Granularity.P1D);

        // When
        permissionRequestService.createPermissionRequest(request);

        // Then
        verify(outbox, times(2)).commit(any());
    }

    @Test
    void testAuthorizePermissionRequest_acceptsPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.existsById("pid"))
                .thenReturn(true);

        // When
        permissionRequestService.authorizePermissionRequest("pid", "upid");

        // Then
        verify(outbox).commit(argThat(event -> event.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        verify(outbox).commit(isA(FrAcceptedEvent.class));
    }

    @Test
    void testAuthorizePermissionRequestWithNullUsageId_rejectsPermissionRequest() throws PermissionNotFoundException {
        // Given
        when(repository.existsById("pid"))
                .thenReturn(true);

        // When
        permissionRequestService.authorizePermissionRequest("pid", null);

        // Then
        verify(outbox).commit(argThat(event -> event.status() == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        verify(outbox).commit(argThat(event -> event.status() == PermissionProcessStatus.REJECTED));
    }

    @Test
    void testAuthorizePermissionRequest_withNonExistingPermissionRequest_throws() {
        // Given, When, Then
        assertThrows(PermissionNotFoundException.class,
                     () -> permissionRequestService.authorizePermissionRequest("NonExistingPid", "upid"));
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
                                                  ZonedDateTime.now(ZoneOffset.UTC));
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
                                                  ZonedDateTime.now(ZoneOffset.UTC));
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
        when(dataNeedsService.findById(any())).thenReturn(Optional.of(mockVhdDataNeed));
        when(mockVhdDataNeed.duration()).thenReturn(absoluteDuration);
        when(absoluteDuration.start()).thenReturn(LocalDate.now(ZONE_ID_FR));
        when(absoluteDuration.end()).thenReturn(LocalDate.now(ZONE_ID_FR).plusDays(10));
        when(mockVhdDataNeed.minGranularity()).thenReturn(Granularity.P1Y);
        PermissionRequestForCreation create = new PermissionRequestForCreation("foo", "bar");

        // When, Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(create));
    }
}
