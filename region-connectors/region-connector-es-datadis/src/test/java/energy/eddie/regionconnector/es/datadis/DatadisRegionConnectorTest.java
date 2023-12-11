package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.es.datadis.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DatadisRegionConnectorTest {
    @Test
    void terminatePermission_callsService() throws PermissionNotFoundException, StateTransitionException {
        // Given
        Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> recordsSink = Sinks.many().unicast().onBackpressureBuffer();
        var mockService = mock(PermissionRequestService.class);
        var permissionId = "SomeId";

        try (var connector = new DatadisRegionConnector(statusMessageSink, recordsSink, mockService, 0)) {

            // When
            assertDoesNotThrow(() -> connector.terminatePermission(permissionId));

            // Then
            verify(mockService).terminatePermission(permissionId);
        }
    }


    @Test
    void getMetadata_returnExpectedMetadata() {
        // Given
        Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> recordsSink = Sinks.many().unicast().onBackpressureBuffer();
        var mockService = mock(PermissionRequestService.class);

        try (var connector = new DatadisRegionConnector(statusMessageSink, recordsSink, mockService, 0)) {

            // When
            var result = connector.getMetadata();

            // then
            assertEquals(DatadisRegionConnectorMetadata.getInstance(), result);
        }
    }

    @Test
    void health_returnsHealthChecks() {
        // Given
        Sinks.Many<ConnectionStatusMessage> statusMessageSink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> recordsSink = Sinks.many().unicast().onBackpressureBuffer();
        var mockService = mock(PermissionRequestService.class);

        try (var connector = new DatadisRegionConnector(statusMessageSink, recordsSink, mockService, 0)) {
            var res = connector.health();

            assertEquals(Map.of("permissionRequestRepository", HealthState.UP), res);
        }
    }
}