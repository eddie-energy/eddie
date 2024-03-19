package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisPendingAcknowledgmentState;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnector.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PermissionRequestServiceTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private PermissionRequestService permissionRequestService;
    @Autowired
    private PermissionRequestRepository<FrEnedisPermissionRequest> repository;
    @MockBean
    private HistoricalDataService historicalDataService;
    @MockBean
    private DataNeedsService dataNeedsService;
    @Mock
    private ValidatedHistoricalDataDataNeed mockVhdDataNeed;

    @Test
    void testCreatePermissionRequest_createsPermissionRequest() throws StateTransitionException, DataNeedNotFoundException, UnsupportedDataNeedException {
        // Given
        var request = new PermissionRequestForCreation("cid", "dnid");
        var wrapper = new DataNeedWrapper(mockVhdDataNeed,
                                          LocalDate.now(ZONE_ID_FR),
                                          LocalDate.now(ZONE_ID_FR).plusDays(10));
        when(dataNeedsService.findDataNeedAndCalculateStartAndEnd(any(),
                                                                  any(),
                                                                  any(),
                                                                  any())).thenReturn(wrapper);
        when(mockVhdDataNeed.minGranularity()).thenReturn(Granularity.P1D);

        // When
        var res = permissionRequestService.createPermissionRequest(request);
        var permissionRequest = repository.findByPermissionId(res.permissionId());

        // Then
        assertTrue(permissionRequest.isPresent());
        assertEquals("cid", permissionRequest.get().connectionId());
        assertEquals(PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT,
                     permissionRequest.get().status());
    }

    @Test
    @DirtiesContext
    void testAuthorizePermissionRequest_acceptsPermissionRequest() throws StateTransitionException, PermissionNotFoundException {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC);
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        request.changeState(new FrEnedisPendingAcknowledgmentState(request, factory));
        repository.save(request);


        // When
        permissionRequestService.authorizePermissionRequest("pid", "upid");
        // Then
        FrEnedisPermissionRequest updatedRequest = repository.findByPermissionId("pid").orElseThrow();
        assertEquals(PermissionProcessStatus.ACCEPTED, updatedRequest.status());
    }

    @Test
    @DirtiesContext
    void testAuthorizePermissionRequestWithNullUsageId_rejectsPermissionRequest() throws StateTransitionException, PermissionNotFoundException {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC);
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        request.changeState(new FrEnedisPendingAcknowledgmentState(request, factory));
        repository.save(request);


        // When
        permissionRequestService.authorizePermissionRequest("pid", null);
        // Then
        FrEnedisPermissionRequest updatedRequest = repository.findByPermissionId("pid").orElseThrow();
        assertEquals(PermissionProcessStatus.REJECTED, updatedRequest.status());
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
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC);
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        repository.save(request);

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
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC);
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        repository.save(request);

        // When
        var res = permissionRequestService.findConnectionStatusMessageById("asdfasdfadsf");

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    @DirtiesContext
    void testFindPermissionRequestById_returnsPermissionRequest() {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC);
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        repository.save(request);

        // When
        var res = permissionRequestService.findPermissionRequestByPermissionId("pid");

        // Then
        assertTrue(res.isPresent());
        assertEquals(request.permissionId(), res.get().permissionId());
    }

    @Test
    @DirtiesContext
    void testFindPermissionRequestById_withNotExistingPermissionId_returnsEmpty() {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC);
        StateBuilderFactory factory = new StateBuilderFactory();
        var request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        repository.save(request);

        // When
        var res = permissionRequestService.findPermissionRequestByPermissionId("asdfasdfadsf");

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    @DirtiesContext
    void testFindTimedOutPermissionRequests_returnsPermissionRequests() throws StateTransitionException {
        // Given
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(3);
        var end = ZonedDateTime.now(ZoneOffset.UTC);
        StateBuilderFactory factory = new StateBuilderFactory();
        var request1 = new EnedisPermissionRequest("pid", "cid", "dnid", start, end, Granularity.P1D, factory);
        var request2 = new EnedisPermissionRequest("pid2", "cid2", "dnid", start, end, Granularity.P1D, factory);
        request1.validate();
        request1.sendToPermissionAdministrator();
        repository.save(request1);
        repository.save(request2);

        // When
        var res = permissionRequestService.findTimedOutPermissionRequests(0);

        // Then
        assertEquals(1, res.size());
    }

    @Test
    void givenUnsupportedGranularity_throws() throws DataNeedNotFoundException {
        // Given
        var wrapper = new DataNeedWrapper(mockVhdDataNeed,
                                          LocalDate.now(ZONE_ID_FR),
                                          LocalDate.now(ZONE_ID_FR).plusDays(10));
        when(dataNeedsService.findDataNeedAndCalculateStartAndEnd(any(),
                                                                  any(),
                                                                  any(),
                                                                  any())).thenReturn(wrapper);
        when(mockVhdDataNeed.minGranularity()).thenReturn(Granularity.P1Y);
        PermissionRequestForCreation create = new PermissionRequestForCreation("foo", "bar");

        // When, Then
        assertThrows(UnsupportedDataNeedException.class,
                     () -> permissionRequestService.createPermissionRequest(create));
    }
}
