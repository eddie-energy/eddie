package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DatadisPermissionRequestTest {
    // Those are valid values
    private final String permissionId = "Existing";
    private final Granularity granularity = Granularity.PT15M;
    private final LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
    private final LocalDate requestDataFrom = now.minusDays(10);
    private final LocalDate requestDataTo = now.minusDays(5);
    private StateBuilderFactory factory;
    private PermissionRequestForCreation requestForCreation;

    @BeforeEach
    void setUp() {
        factory = new StateBuilderFactory(mock(AuthorizationApi.class));
        String connectionId = "connId";
        String dataNeedId = "dataNeed";
        String meteringPointId = "7890";
        String nif = "123456";
        requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId);
    }

    @Test
    void givenValidInput_constructor_requestIsInCreatedState() {
        DatadisPermissionRequest request = new DatadisPermissionRequest(permissionId,
                                                                        requestForCreation,
                                                                        requestDataFrom,
                                                                        requestDataTo,
                                                                        granularity,
                                                                        factory);

        assertEquals(PermissionProcessStatus.CREATED, request.state().status());
    }

    @Test
    void givenNull_constructor_throws() {
        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(null,
                                                                                    requestForCreation,
                                                                                    requestDataFrom,
                                                                                    requestDataTo,
                                                                                    granularity,
                                                                                    factory));

        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(permissionId,
                                                                                    null,
                                                                                    requestDataFrom,
                                                                                    requestDataTo,
                                                                                    granularity,
                                                                                    factory));

        assertThrows(NullPointerException.class, () -> new DatadisPermissionRequest(permissionId,
                                                                                    requestForCreation,
                                                                                    requestDataFrom,
                                                                                    requestDataTo,
                                                                                    granularity,
                                                                                    null));
    }


    @Test
    void lastPulledMeterReading_whenConstructed_isEmpty() {
        var request = new DatadisPermissionRequest(permissionId,
                                                   requestForCreation,
                                                   requestDataFrom,
                                                   requestDataTo,
                                                   granularity,
                                                   factory);
        assertTrue(request.latestMeterReadingEndDate().isEmpty());
    }

    @Test
    void distributorCode_whenConstructed_isEmpty() {
        var request = new DatadisPermissionRequest(permissionId,
                                                   requestForCreation,
                                                   requestDataFrom,
                                                   requestDataTo,
                                                   granularity,
                                                   factory);

        assertTrue(request.distributorCode().isEmpty());
    }

    @Test
    void pointType_whenConstructed_IsEmpty() {
        var request = new DatadisPermissionRequest(permissionId,
                                                   requestForCreation,
                                                   requestDataFrom,
                                                   requestDataTo,
                                                   granularity,
                                                   factory);

        assertTrue(request.pointType().isEmpty());
    }

    @Test
    void setLastPulledMeterReading_worksAsExpected() {
        // Given
        var request = new DatadisPermissionRequest(permissionId,
                                                   requestForCreation,
                                                   requestDataFrom,
                                                   requestDataTo,
                                                   granularity,
                                                   factory);
        LocalDate expected = LocalDate.now(ZoneOffset.UTC);

        // When
        request.updateLatestMeterReadingEndDate(expected);

        // Then
        assertTrue(request.latestMeterReadingEndDate().isPresent());
        assertEquals(expected, request.latestMeterReadingEndDate().get());
    }

    @Test
    void setDistributorCodeAndPointType_worksAsExpected() {
        // Given
        var request = new DatadisPermissionRequest(permissionId,
                                                   requestForCreation,
                                                   requestDataFrom,
                                                   requestDataTo,
                                                   granularity,
                                                   factory);
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
