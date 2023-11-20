package energy.eddie.regionconnector.fr.enedis.client;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthCheckedEnedisApiTest {
    @Test
    void testPostToken_doesNotThrow() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        doNothing().when(mockedEnedisApi).postToken();

        // When
        // Then
        assertDoesNotThrow(healthCheckedEnedisApi::postToken);
    }

    @Test
    void testPostToken_throws() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        doThrow(new ApiException("Authentication failed")).when(mockedEnedisApi).postToken();

        // When
        assertThrows(ApiException.class, healthCheckedEnedisApi::postToken);
    }

    @Test
    void testPostToken_authEndpointUp() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        doNothing().when(mockedEnedisApi).postToken();
        healthCheckedEnedisApi.postToken();

        // When
        var res = healthCheckedEnedisApi.health();

        // Then
        assertEquals(HealthState.UP, res.get(HealthCheckedEnedisApi.AUTHENTICATION_API));
    }

    @Test
    void testPostToken_authEndpointDown() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        doThrow(new ApiException("Authentication failed")).when(mockedEnedisApi).postToken();
        try {
            healthCheckedEnedisApi.postToken();
        } catch (ApiException ignored) {
        }

        // When
        var res = healthCheckedEnedisApi.health();

        // Then
        assertEquals(HealthState.DOWN, res.get(HealthCheckedEnedisApi.AUTHENTICATION_API));
    }

    @Test
    void testGetDailyConsumption_doesNotThrow() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        ConsumptionRecord dummyRecord = new ConsumptionRecord();
        when(mockedEnedisApi.getDailyConsumption(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(dummyRecord);

        // When
        // Then
        assertDoesNotThrow(() ->
                healthCheckedEnedisApi.getDailyConsumption(
                        "123",
                        ZonedDateTime.now(ZoneId.systemDefault()),
                        ZonedDateTime.now(ZoneId.systemDefault())
                )
        );
    }

    @Test
    void testGetDailyConsumption_throws() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        when(mockedEnedisApi.getDailyConsumption(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenThrow(new ApiException("Metering point not found"));

        // When
        // Then
        assertThrows(ApiException.class, () ->
                healthCheckedEnedisApi.getDailyConsumption(
                        "123",
                        ZonedDateTime.now(ZoneId.systemDefault()),
                        ZonedDateTime.now(ZoneId.systemDefault())
                )
        );
    }

    @Test
    void testGetDailyConsumption_meteringEndpointUp() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        ConsumptionRecord dummyRecord = new ConsumptionRecord();
        when(mockedEnedisApi.getDailyConsumption(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(dummyRecord);
        healthCheckedEnedisApi.getDailyConsumption(
                "123",
                ZonedDateTime.now(ZoneId.systemDefault()),
                ZonedDateTime.now(ZoneId.systemDefault())
        );

        // When
        var res = healthCheckedEnedisApi.health();

        // Then
        assertEquals(HealthState.UP, res.get(HealthCheckedEnedisApi.METERING_POINT_API));
    }

    @Test
    void testGetDailyConsumption_meteringEndpointDown() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        when(mockedEnedisApi.getDailyConsumption(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenThrow(new ApiException("Metering point not found"));
        try {
            healthCheckedEnedisApi.getDailyConsumption(
                    "123",
                    ZonedDateTime.now(ZoneId.systemDefault()),
                    ZonedDateTime.now(ZoneId.systemDefault())
            );
        } catch (ApiException ignored) {
        }

        // When
        var res = healthCheckedEnedisApi.health();

        // Then
        assertEquals(HealthState.DOWN, res.get(HealthCheckedEnedisApi.METERING_POINT_API));
    }

    @Test
    void testGetConsumptionLoadCurve_doesNotThrow() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        ConsumptionRecord dummyRecord = new ConsumptionRecord();
        when(mockedEnedisApi.getConsumptionLoadCurve(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(dummyRecord);

        // When
        // Then
        assertDoesNotThrow(() ->
                healthCheckedEnedisApi.getConsumptionLoadCurve(
                        "123",
                        ZonedDateTime.now(ZoneId.systemDefault()),
                        ZonedDateTime.now(ZoneId.systemDefault())
                )
        );
    }

    @Test
    void testGetConsumptionLoadCurve_throws() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        when(mockedEnedisApi.getConsumptionLoadCurve(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenThrow(new ApiException("Metering point not found"));

        // When
        // Then
        assertThrows(ApiException.class, () ->
                healthCheckedEnedisApi.getConsumptionLoadCurve(
                        "123",
                        ZonedDateTime.now(ZoneId.systemDefault()),
                        ZonedDateTime.now(ZoneId.systemDefault()))
        );
    }

    @Test
    void testGetConsumptionLoadCurve_meteringEndpointUp() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        ConsumptionRecord dummyRecord = new ConsumptionRecord();
        when(mockedEnedisApi.getConsumptionLoadCurve(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenReturn(dummyRecord);
        healthCheckedEnedisApi.getConsumptionLoadCurve(
                "123",
                ZonedDateTime.now(ZoneId.systemDefault()),
                ZonedDateTime.now(ZoneId.systemDefault())
        );

        // When
        var res = healthCheckedEnedisApi.health();

        // Then
        assertEquals(HealthState.UP, res.get(HealthCheckedEnedisApi.METERING_POINT_API));
    }

    @Test
    void testGetConsumptionLoadCurve_meteringEndpointDown() throws ApiException {
        // Given
        EnedisApi mockedEnedisApi = mock(EnedisApi.class);
        HealthCheckedEnedisApi healthCheckedEnedisApi = new HealthCheckedEnedisApi(mockedEnedisApi);
        when(mockedEnedisApi.getConsumptionLoadCurve(anyString(), any(ZonedDateTime.class), any(ZonedDateTime.class)))
                .thenThrow(new ApiException("Metering point not found"));
        try {
            healthCheckedEnedisApi.getConsumptionLoadCurve(
                    "123",
                    ZonedDateTime.now(ZoneId.systemDefault()),
                    ZonedDateTime.now(ZoneId.systemDefault())
            );
        } catch (ApiException ignored) {
        }

        // When
        var res = healthCheckedEnedisApi.health();

        // Then
        assertEquals(HealthState.DOWN, res.get(HealthCheckedEnedisApi.METERING_POINT_API));
    }

}