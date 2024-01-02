package energy.eddie.regionconnector.dk.energinet.customer.client;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnerginetCustomerApiClientTest {
    private static final ZoneId DK_ZONE_ID = EnerginetRegionConnector.DK_ZONE_ID;
    private static final int MAX_PERIOD = 730;

    @Test
    void health_returnHealthUpState() {
        // Given
        var config = mock(EnerginetConfiguration.class);
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
    void health_returnHealthDownState() {
        // Given
        var config = mock(EnerginetConfiguration.class);
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
    void apiToken_refreshTokenNotSet_throws() {
        // Given
        var config = mock(EnerginetConfiguration.class);
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
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);

        // When
        // Then
        assertThrows(IllegalStateException.class, () -> client.getTimeSeries(start, end, Granularity.PT15M, meteringPointsRequest));
    }

    @Test
    void getTimeSeries_invalidTimeFrame_throws() {
        // Given
        var endBeforeStart = ZonedDateTime.of(LocalDate.of(2023, 1, 1).atStartOfDay(), DK_ZONE_ID);
        var today = ZonedDateTime.of(LocalDate.now(ZoneId.systemDefault()).atStartOfDay(), DK_ZONE_ID);
        var start = ZonedDateTime.of(LocalDate.of(2023, 2, 1).atStartOfDay(), DK_ZONE_ID);

        Granularity granularity = mock(Granularity.class);
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);

        // When
        // Then
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(start, endBeforeStart, granularity, meteringPointsRequest));
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(start, today, granularity, meteringPointsRequest));
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(today, today, granularity, meteringPointsRequest));
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(start, start, granularity, meteringPointsRequest));
    }

    @Test
    void getTimeSeries_invalidTimeFrame_exceedMaxPeriod_throws() {
        // Given
        var end = ZonedDateTime.of(LocalDate.now(ZoneId.systemDefault()).minusDays(1).atStartOfDay(), DK_ZONE_ID);
        var startExceedsMaxPeriod = end.minusDays(MAX_PERIOD + 1);
        Granularity granularity = mock(Granularity.class);
        var meteringPointsRequest = mock(MeteringPointsRequest.class);
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("path");

        var client = new EnerginetCustomerApiClient(config);

        // When
        // Then
        assertThrows(DateTimeException.class, () -> client.getTimeSeries(startExceedsMaxPeriod, end, granularity, meteringPointsRequest));
    }
}