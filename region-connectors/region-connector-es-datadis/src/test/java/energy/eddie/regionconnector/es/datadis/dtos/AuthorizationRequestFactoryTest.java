package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AuthorizationRequestFactoryTest {
    private final String permissionId = "Existing";
    private final String connectionId = "connId";
    private final String dataNeedId = "dataNeed";
    private final String nif = "123456";
    private final String meteringPointId = "7890";
    private final Granularity granularity = Granularity.PT15M;
    private final LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
    private final LocalDate requestDataFrom = now.minusDays(10);
    @Mock
    private AuthorizationApi authorizationApi;

    private final StateBuilderFactory factory = new StateBuilderFactory(authorizationApi);


    @Test
    void endDate_whenRequestingFutureData_IsTheSameAsRequestDataTo() {
        var futureDate = LocalDate.now(ZoneOffset.UTC).plusMonths(1);
        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId);
        var request = new DatadisPermissionRequest(permissionId,
                                                   requestForCreation,
                                                   requestDataFrom,
                                                   futureDate,
                                                   granularity,
                                                   factory);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.fromPermissionRequest(request);

        assertEquals(request.end(), authorizationRequest.endDate());
    }

    @Test
    void endDate_whenRequestingPastData_isOneDayGraterThanAuthorizationStart() {
        var pastDate = LocalDate.now(ZoneOffset.UTC).minusMonths(1);
        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId);
        var request = new DatadisPermissionRequest(permissionId,
                                                   requestForCreation,
                                                   requestDataFrom,
                                                   pastDate,
                                                   granularity,
                                                   factory);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.fromPermissionRequest(request);

        assertEquals(authorizationRequest.startDate().plusDays(1), authorizationRequest.endDate());
    }

    @Test
    void endDate_whenRequestingToday_isOneDayGraterThanAuthorizationStart() {
        var today = LocalDate.now(ZoneOffset.UTC);
        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif, meteringPointId);
        var request = new DatadisPermissionRequest(permissionId,
                                                   requestForCreation,
                                                   today,
                                                   today,
                                                   granularity,
                                                   factory);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.fromPermissionRequest(request);

        assertEquals(request.start().plusDays(1), authorizationRequest.endDate());
    }
}
