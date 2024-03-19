package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeoutServiceTest {
    @Mock
    private PermissionRequestService permissionRequestService;

    @Test
    void testTimeoutPendingPermissionRequests_transitionsRequests() throws StateTransitionException {
        // Given
        var start = ZonedDateTime.now(ZONE_ID_FR);
        var end = ZonedDateTime.now(ZONE_ID_FR).plusDays(10);
        EnedisConfiguration config = new PlainEnedisConfiguration("clientId", "clientSecret", "/path", 24);
        StateBuilderFactory factory = new StateBuilderFactory();
        EnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end,
                                                                      Granularity.PT15M,
                                                                      factory);
        request.validate();
        request.sendToPermissionAdministrator();
        when(permissionRequestService.findTimedOutPermissionRequests(24))
                .thenReturn(List.of(request));
        TimeoutService timeoutService = new TimeoutService(config, permissionRequestService);

        // When
        timeoutService.timeoutPendingPermissionRequests();

        // Then
        assertEquals(PermissionProcessStatus.TIMED_OUT, request.status());
    }

    @Test
    void testTimeoutPendingPermissionRequests_doesNotThrow() {
        // Given
        var start = ZonedDateTime.now(ZONE_ID_FR);
        var end = ZonedDateTime.now(ZONE_ID_FR).plusDays(10);
        EnedisConfiguration config = new PlainEnedisConfiguration("clientId", "clientSecret", "/path", 24);
        StateBuilderFactory factory = new StateBuilderFactory();
        EnedisPermissionRequest request = new EnedisPermissionRequest("pid", "cid", "dnid", start, end,
                                                                      Granularity.PT15M,
                                                                      factory);
        when(permissionRequestService.findTimedOutPermissionRequests(24))
                .thenReturn(List.of(request));
        TimeoutService timeoutService = new TimeoutService(config, permissionRequestService);

        // When
        // Then
        assertDoesNotThrow(timeoutService::timeoutPendingPermissionRequests);
    }

}
