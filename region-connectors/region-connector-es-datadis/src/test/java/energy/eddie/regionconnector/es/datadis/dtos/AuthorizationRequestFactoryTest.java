package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.DatadisPermissionRequestBuilder;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationRequestFactoryTest {
    private final Granularity granularity = Granularity.PT15M;
    private final LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
    private final LocalDate requestDataFrom = now.minusDays(10);


    @Test
    void endDate_whenRequestingFutureData_IsOneDayGreaterToAccountForExclusivity() {
        var futureDate = LocalDate.now(ZoneOffset.UTC).plusMonths(1);
        var request = getPermissionRequest(futureDate);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.fromPermissionRequest(request);

        assertEquals(request.end().plusDays(1), authorizationRequest.endDate());
    }

    @Test
    void endDate_whenRequestingPastData_isOneDayGraterThanAuthorizationStart() {
        var pastDate = LocalDate.now(ZoneOffset.UTC).minusMonths(1);
        var request = getPermissionRequest(pastDate);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.fromPermissionRequest(request);

        assertEquals(authorizationRequest.startDate().plusDays(1), authorizationRequest.endDate());
    }

    @Test
    void endDate_whenRequestingToday_isOneDayGraterThanAuthorizationStart() {
        var today = LocalDate.now(ZoneOffset.UTC);
        var request = getPermissionRequest(today);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.fromPermissionRequest(request);

        assertEquals(request.end().plusDays(1), authorizationRequest.endDate());
    }

    private DatadisPermissionRequest getPermissionRequest(LocalDate today) {
        return new DatadisPermissionRequestBuilder()
                .setPermissionId("Existing")
                .setConnectionId("connId")
                .setDataNeedId("dataNeed")
                .setGranularity(granularity)
                .setNif("123456")
                .setMeteringPointId("7890")
                .setStart(requestDataFrom)
                .setEnd(today)
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .setCreated(ZonedDateTime.now(ZoneOffset.UTC))
                .setAllowedGranularity(AllowedGranularity.PT15M_OR_PT1H)
                .build();
    }
}
