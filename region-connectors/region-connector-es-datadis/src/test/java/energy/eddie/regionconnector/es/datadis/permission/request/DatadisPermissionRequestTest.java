package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationResponseHandler;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DatadisPermissionRequestTest {
    // Those are valid values
    private final String permissionId = "Existing";
    private final String connectionId = "connId";
    private final String dataNeedId = "dataNeed";
    private final String nif = "123456";
    private final String meteringPointId = "7890";
    private final MeasurementType measurementType = MeasurementType.QUARTER_HOURLY;
    private final ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
    private final ZonedDateTime requestDataFrom = now.minusDays(10);
    private final ZonedDateTime requestDataTo = now.minusDays(5);
    @Mock
    private AuthorizationApi authorizationApi;
    @Mock
    private AuthorizationResponseHandler authorizationResponseHandler;
    private PermissionRequestForCreation requestForCreation;

    @BeforeEach
    void setUp() {
        requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, measurementType);
    }

    @Test
    void givenValidInput_constructor_requestIsInCreatedState() {
        DatadisPermissionRequest request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);

        assertEquals(PermissionProcessStatus.CREATED, request.state().status());
    }

    @Test
    void givenNull_constructor_throws() {
        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(null, requestForCreation,
                authorizationApi, authorizationResponseHandler));

        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(permissionId, null,
                authorizationApi, authorizationResponseHandler));

        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(permissionId, requestForCreation,
                null, authorizationResponseHandler));

        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, null));
    }

    @Test
    @Disabled("Future data isn't supported yet")
    void permissionEnd_whenRequestingFutureData_IsTheSameAsRequestDataTo() {
        var futureDate = ZonedDateTime.now(ZoneOffset.UTC).plusMonths(1);
        requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, futureDate, measurementType);

        var request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);
        assertEquals(request.requestDataTo(), request.permissionEnd());
    }

    @Test
    void permissionEnd_whenRequestingPastData_isTheSameAsPermissionStart() {
        var pastDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1);
        requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, pastDate, measurementType);

        var request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);

        assertEquals(request.permissionStart(), request.permissionEnd());
    }

    @Test
    void lastPulledMeterReading_whenConstructed_isEmpty() {
        var request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);
        assertTrue(request.lastPulledMeterReading().isEmpty());
    }

    @Test
    void distributorCode_whenConstructed_isEmpty() {
        var request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);

        assertTrue(request.distributorCode().isEmpty());
    }

    @Test
    void pointType_whenConstructed_IsEmpty() {
        var request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);

        assertTrue(request.pointType().isEmpty());
    }

    @Test
    void setLastPulledMeterReading_worksAsExpected() {
        // Given
        var request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);
        ZonedDateTime expected = ZonedDateTime.now(ZoneOffset.UTC);

        // When
        request.setLastPulledMeterReading(expected);

        // Then
        assertTrue(request.lastPulledMeterReading().isPresent());
        assertEquals(expected, request.lastPulledMeterReading().get());
    }

    @Test
    void setDistributorCode_worksAsExpected() {
        // Given
        var request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);
        var expected = "distributorCode";

        // When
        request.setDistributorCode(expected);

        // Then
        assertTrue(request.distributorCode().isPresent());
        assertEquals(expected, request.distributorCode().get());
    }

    @Test
    void setPointType_worksAsExpected() {
        // Given
        var request = new DatadisPermissionRequest(permissionId, requestForCreation,
                authorizationApi, authorizationResponseHandler);
        var expected = 1;

        // When
        request.setPointType(expected);

        // Then
        assertTrue(request.pointType().isPresent());
        assertEquals(expected, request.pointType().get());
    }
}