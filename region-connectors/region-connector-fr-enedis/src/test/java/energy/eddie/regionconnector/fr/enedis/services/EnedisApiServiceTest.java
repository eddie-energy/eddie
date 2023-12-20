package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
class EnedisApiServiceTest {
    @MockBean
    private EnedisApi mockEnedisApi;
    @Autowired
    private EnedisApiService enedisApiService;
    @MockBean
    private ServletWebServerApplicationContext unused;

    @Test
    void testGetDailyConsumption_withRetry() throws ApiException {
        //Given
        String usagePointId = "123";
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        ConsumptionRecord mockedRecord = new ConsumptionRecord();
        when(mockEnedisApi.getDailyConsumption(usagePointId, start, end))
                .thenThrow(new ApiException())
                .thenReturn(mockedRecord);

        //When
        ConsumptionRecord result = enedisApiService.getDailyConsumption(usagePointId, start, end);

        //Then
        assertEquals(mockedRecord, result);
    }

    @Test
    void testGetConsumptionLoadCurve_withRetry() throws ApiException {
        //Given
        String usagePointId = "123";
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        ConsumptionRecord mockedRecord = new ConsumptionRecord();
        when(mockEnedisApi.getConsumptionLoadCurve(usagePointId, start, end))
                .thenThrow(new ApiException())
                .thenReturn(mockedRecord);

        //When
        ConsumptionRecord result = enedisApiService.getConsumptionLoadCurve(usagePointId, start, end);

        //Then
        assertEquals(mockedRecord, result);
    }
}