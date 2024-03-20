package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DatadisPermissionRequestTest {
    // Those are valid values
    private final String permissionId = "Existing";
    private final String connectionId = "connId";
    private final String dataNeedId = "dataNeed";
    private final String nif = "123456";
    private final String meteringPointId = "7890";
    private final Granularity granularity = Granularity.PT15M;
    private final ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
    private final ZonedDateTime requestDataFrom = now.minusDays(10);
    private final ZonedDateTime requestDataTo = now.minusDays(5);
    private StateBuilderFactory factory;
    private PermissionRequestForCreation requestForCreation;

    @BeforeEach
    void setUp() {
        factory = new StateBuilderFactory(mock(AuthorizationApi.class));
        requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, requestDataTo, granularity);
    }

    @Test
    void givenValidInput_constructor_requestIsInCreatedState() {
        DatadisPermissionRequest request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);

        assertEquals(PermissionProcessStatus.CREATED, request.state().status());
    }

    @Test
    void givenNull_constructor_throws() {
        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(null, requestForCreation, factory));

        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(permissionId, null, factory));

        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(permissionId, requestForCreation, null));
    }

    @Test
    @Disabled("Future data isn't supported yet")
    void permissionEnd_whenRequestingFutureData_IsTheSameAsRequestDataTo() {
        var futureDate = ZonedDateTime.now(ZoneOffset.UTC).plusMonths(1);
        requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, futureDate, granularity);

        var request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);
        assertEquals(request.end(), request.permissionEnd().toLocalDate());
    }

    @Test
    void permissionEnd_whenRequestingPastData_isOneDayGraterThanPermissionStart() {
        var pastDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1);
        requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                requestDataFrom, pastDate, granularity);

        var request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);

        assertEquals(request.permissionStart().plusDays(1), request.permissionEnd());
    }

    @Test
    void permissionEnd_whenRequestingTodaysData_isOneDayGraterThanPermissionStart() {
        var today = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
        requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId,
                today, today, granularity);

        var request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);

        assertEquals(request.permissionStart().toLocalDate().plusDays(1), request.permissionEnd().toLocalDate());
    }

    @Test
    void lastPulledMeterReading_whenConstructed_isEmpty() {
        var request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);
        assertTrue(request.lastPulledMeterReading().isEmpty());
    }

    @Test
    void distributorCode_whenConstructed_isEmpty() {
        var request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);

        assertTrue(request.distributorCode().isEmpty());
    }

    @Test
    void pointType_whenConstructed_IsEmpty() {
        var request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);

        assertTrue(request.pointType().isEmpty());
    }

    @Test
    void setLastPulledMeterReading_worksAsExpected() {
        // Given
        var request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);
        LocalDate expected = LocalDate.now(ZoneOffset.UTC);

        // When
        request.updateLastPulledMeterReading(expected);

        // Then
        assertTrue(request.lastPulledMeterReading().isPresent());
        assertEquals(expected, request.lastPulledMeterReading().get());
    }

    @Test
    void setDistributorCodeAndPointType_worksAsExpected() {
        // Given
        var request = new DatadisPermissionRequest(permissionId, requestForCreation, factory);
        DistributorCode expectedDistributorCode = DistributorCode.VIESGO;
        var expectedPointType = 1;


        // When
        request.setDistributorCodeAndPointType(expectedDistributorCode, expectedPointType);

        // Then
        assertTrue(request.distributorCode().isPresent());
        assertEquals(expectedDistributorCode, request.distributorCode().get());
        assertTrue(request.pointType().isPresent());
    }
}
