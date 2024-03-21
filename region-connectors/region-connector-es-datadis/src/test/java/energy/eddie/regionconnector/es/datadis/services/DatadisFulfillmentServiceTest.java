package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;

class DatadisFulfillmentServiceTest {
    private static final LocalDate today = LocalDate.now(ZONE_ID_SPAIN);

    @Test
    void isPermissionRequestFulfilledByDate_ifEndBeforePermissionEnd_returnsFalse() {
        // Given
        EsPermissionRequest permissionRequest = permissionRequest(today.minusDays(2),
                                                                  today,
                                                                  PermissionProcessStatus.ACCEPTED);
        LocalDate end = today.minusDays(1);

        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        var fulfills = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, end);

        // Then
        assertFalse(fulfills);
    }

    private static EsPermissionRequest permissionRequest(
            LocalDate start,
            LocalDate end,
            PermissionProcessStatus permissionProcessStatus
    ) {
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId");
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId",
                                                                             permissionRequestForCreation,
                                                                             start,
                                                                             end,
                                                                             Granularity.PT1H,
                                                                             stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, permissionProcessStatus).build());
        return permissionRequest;
    }

    @Test
    void isPermissionRequestFulfilledByDate_ifEndAfterPermissionEnd_returnsTrue() {
        // Given
        EsPermissionRequest permissionRequest = permissionRequest(today.minusDays(2),
                                                                  today.minusDays(1),
                                                                  PermissionProcessStatus.ACCEPTED);

        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        var fulfills = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, today);

        // Then
        assertTrue(fulfills);
    }

    @Test
    void isPermissionRequestFulfilledByDate_ifEndEqualPermissionEnd_returnsTrue() {
        // Given
        LocalDate start = today.minusDays(2);
        EsPermissionRequest permissionRequest = permissionRequest(start,
                                                                  today.minusDays(1),
                                                                  PermissionProcessStatus.ACCEPTED); // DatadisPermissionRequest adds 1 day to the end date


        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        var fulfills = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, today);
        // Then
        assertTrue(fulfills);
    }

    @Test
    void tryFulfillPermissionRequest_changesStateFromAcceptedToFulfilled() {
        // Given
        LocalDate start = today.minusDays(2);
        EsPermissionRequest permissionRequest = permissionRequest(start,
                                                                  today.minusDays(1),
                                                                  PermissionProcessStatus.ACCEPTED);


        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        fulfillmentService.tryFulfillPermissionRequest(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.FULFILLED, permissionRequest.status());
    }

    @Test
    void tryFulfillPermissionRequest_doesNotChangeStateIfNotAccepted() {
        // Given
        LocalDate start = today.minusDays(2);
        EsPermissionRequest permissionRequest = permissionRequest(start,
                                                                  today.minusDays(1),
                                                                  PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);


        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        fulfillmentService.tryFulfillPermissionRequest(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, permissionRequest.status());
    }
}
