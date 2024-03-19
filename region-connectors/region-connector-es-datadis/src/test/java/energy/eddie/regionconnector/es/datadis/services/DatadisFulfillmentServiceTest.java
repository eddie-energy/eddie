package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;

class DatadisFulfillmentServiceTest {
    private static final ZonedDateTime today = LocalDate.now(ZONE_ID_SPAIN).atStartOfDay(ZONE_ID_SPAIN);

    private static EsPermissionRequest permissionRequest(ZonedDateTime start, ZonedDateTime end, PermissionProcessStatus permissionProcessStatus) {
        StateBuilderFactory stateBuilderFactory = new StateBuilderFactory(null);
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation(
                "connectionId",
                "dataNeedId",
                "nif",
                "meteringPointId",
                start,
                end,
                Granularity.PT1H);
        EsPermissionRequest permissionRequest = new DatadisPermissionRequest("permissionId", permissionRequestForCreation, stateBuilderFactory);
        permissionRequest.changeState(stateBuilderFactory.create(permissionRequest, permissionProcessStatus).build());
        return permissionRequest;
    }

    @Test
    void isPermissionRequestFulfilledByDate_ifEndBeforePermissionEnd_returnsFalse() {
        // Given
        EsPermissionRequest permissionRequest = permissionRequest(today.minusDays(2), today.minusDays(1), PermissionProcessStatus.ACCEPTED); // DatadisPermissionRequest adds 1 day to the end date
        ZonedDateTime end = today.minusDays(1);

        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        var fulfills = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, end);

        // Then
        assertFalse(fulfills);
    }

    @Test
    void isPermissionRequestFulfilledByDate_ifEndAfterPermissionEnd_returnsTrue() {
        // Given
        EsPermissionRequest permissionRequest = permissionRequest(today.minusDays(2), today.minusDays(1), PermissionProcessStatus.ACCEPTED);

        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        var fulfills = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, today);

        // Then
        assertTrue(fulfills);
    }

    @Test
    void isPermissionRequestFulfilledByDate_ifEndEqualPermissionEnd_returnsTrue() {
        // Given
        ZonedDateTime start = today.minusDays(2);
        EsPermissionRequest permissionRequest = permissionRequest(start, today.minusDays(1), PermissionProcessStatus.ACCEPTED); // DatadisPermissionRequest adds 1 day to the end date


        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        var fulfills = fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, today);
        // Then
        assertTrue(fulfills);
    }

    @Test
    void tryFulfillPermissionRequest_changesStateFromAcceptedToFulfilled() {
        // Given
        ZonedDateTime start = today.minusDays(2);
        EsPermissionRequest permissionRequest = permissionRequest(start, today.minusDays(1), PermissionProcessStatus.ACCEPTED);


        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        fulfillmentService.tryFulfillPermissionRequest(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.FULFILLED, permissionRequest.status());
    }

    @Test
    void tryFulfillPermissionRequest_doesNotChangeStateIfNotAccepted() {
        // Given
        ZonedDateTime start = today.minusDays(2);
        EsPermissionRequest permissionRequest = permissionRequest(start, today.minusDays(1), PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);


        DatadisFulfillmentService fulfillmentService = new DatadisFulfillmentService();

        // When
        fulfillmentService.tryFulfillPermissionRequest(permissionRequest);

        // Then
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, permissionRequest.status());
    }
}
