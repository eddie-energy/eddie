package energy.eddie.regionconnector.es.datadis.dtos;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationRequestFactoryTest {
    @Test
    void endDate_whenRequestingFutureData_IsOneDayGreaterToAccountForExclusivity() {
        var futureDate = LocalDate.now(ZONE_ID_SPAIN).plusMonths(1);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.from("123456", "7890", futureDate);

        assertEquals(futureDate.plusDays(1), authorizationRequest.endDate());
    }

    @Test
    void endDate_whenRequestingPastData_isOneDayGraterThanAuthorizationStart() {
        var pastDate = LocalDate.now(ZONE_ID_SPAIN).minusMonths(1);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.from("123456", "7890", pastDate);

        assertEquals(authorizationRequest.startDate().plusDays(1), authorizationRequest.endDate());
    }

    @Test
    void endDate_whenRequestingToday_isOneDayGraterThanAuthorizationStart() {
        var today = LocalDate.now(ZONE_ID_SPAIN);

        AuthorizationRequestFactory factory = new AuthorizationRequestFactory();

        AuthorizationRequest authorizationRequest = factory.from("123456", "7890", today);

        assertEquals(today.plusDays(1), authorizationRequest.endDate());
    }
}
