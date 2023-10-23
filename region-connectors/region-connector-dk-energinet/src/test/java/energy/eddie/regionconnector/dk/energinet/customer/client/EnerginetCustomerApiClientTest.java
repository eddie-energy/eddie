package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.config.PropertiesEnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnerginetCustomerApiClientTest {
    private static final ZoneId DK_ZONE_ID = ZoneId.of("Europe/Copenhagen");
    private static final int MAX_PERIOD = 730;

    @Test
    void isAlive_returnTrue() {
        //given
        var energinetCustomerApiClient = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApiClient.isAlive()).thenReturn(true);

        //when
        //then
        assertTrue(energinetCustomerApiClient.isAlive());
    }

    @Test
    void health_returnHealthUpState() {
        // Given
        var config = mock(PropertiesEnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);
        var spy = spy(client);
        doReturn(true).when(spy).isAlive();

        // When
        Map<String, HealthState> actualHealth = spy.health();

        // Then
        assertTrue(actualHealth.containsKey("isAliveApi"));
        assertEquals(HealthState.UP, actualHealth.get("isAliveApi"));
    }

    @Test
    void isAlive_returnFalse() {
        //given
        var energinetCustomerApiClient = mock(EnerginetCustomerApiClient.class);

        //when
        //then
        assertFalse(energinetCustomerApiClient.isAlive());
    }

    @Test
    void health_returnHealthDownState() {
        // Given
        var config = mock(PropertiesEnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);
        var spy = spy(client);
        doReturn(false).when(spy).isAlive();

        // When
        Map<String, HealthState> actualHealth = spy.health();

        // Then
        assertTrue(actualHealth.containsKey("isAliveApi"));
        assertEquals(HealthState.DOWN, actualHealth.get("isAliveApi"));
    }

    @Test
    void apiToken_refreshTokenSet_asExpected() {
        // Given
        var client = mock(EnerginetCustomerApiClient.class);
        client.setRefreshToken("refreshToken");
        doNothing().when(client).apiToken();

        // When
        // Then
        assertDoesNotThrow(client::apiToken);
    }

    @Test
    void apiToken_refreshTokenNotSet_throws() {
        // Given
        var config = mock(PropertiesEnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);

        // When
        // Then
        assertThrows(IllegalStateException.class, client::apiToken);
    }

    @Test
    void getTimeSeries_refreshTokenIsNotSet_throws() {
        // Given
        var start = ZonedDateTime.of(LocalDate.of(2023, 1, 1).atStartOfDay(), DK_ZONE_ID);
        var end = ZonedDateTime.of(LocalDate.of(2023, 2, 1).atStartOfDay(), DK_ZONE_ID);
        var periodResolution = mock(PeriodResolutionEnum.class);
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var config = mock(PropertiesEnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);

        // When
        // Then
        assertThrows(IllegalStateException.class, () -> client.getTimeSeries(start, end, periodResolution, meteringPointsRequest));
    }

    @Test
    void getTimeSeries_asExpected() {
        // Given
        var end = ZonedDateTime.of(LocalDate.now().minusDays(1).atStartOfDay(), DK_ZONE_ID);
        var start = end.minusDays(1);
        var startWithMaxPeriod = end.minusDays(MAX_PERIOD);

        var periodResolution = mock(PeriodResolutionEnum.class);
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var client = mock(EnerginetCustomerApiClient.class);
        when(client.getTimeSeries(start, end, periodResolution, meteringPointsRequest)).thenReturn(mock(ConsumptionRecord.class));

        // When
        // Then
        assertDoesNotThrow(() -> client.getTimeSeries(start, end, periodResolution, meteringPointsRequest));
        assertDoesNotThrow(() -> client.getTimeSeries(startWithMaxPeriod, end, periodResolution, meteringPointsRequest));
    }

    @Test
    void getTimeSeries_invalidTimeFrame_throws() {
        // Given
        var endBeforeStart = ZonedDateTime.of(LocalDate.of(2023, 1, 1).atStartOfDay(), DK_ZONE_ID);
        var today = ZonedDateTime.of(LocalDate.now().atStartOfDay(), DK_ZONE_ID);
        var start = ZonedDateTime.of(LocalDate.of(2023, 2, 1).atStartOfDay(), DK_ZONE_ID);

        var periodResolution = mock(PeriodResolutionEnum.class);
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var config = mock(PropertiesEnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);

        // When
        // Then
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(start, endBeforeStart, periodResolution, meteringPointsRequest));
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(start, today, periodResolution, meteringPointsRequest));
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(today, today, periodResolution, meteringPointsRequest));
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(start, start, periodResolution, meteringPointsRequest));
    }

    @Test
    void getTimeSeries_invalidTimeFrame_exceedMaxPeriod_throws() {
        // Given
        var end = ZonedDateTime.of(LocalDate.now().minusDays(1).atStartOfDay(), DK_ZONE_ID);
        var startExceedsMaxPeriod = end.minusDays(MAX_PERIOD + 1);
        var periodResolution = mock(PeriodResolutionEnum.class);
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var config = mock(PropertiesEnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);

        // When
        // Then
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(startExceedsMaxPeriod, end, periodResolution, meteringPointsRequest));
    }
}
