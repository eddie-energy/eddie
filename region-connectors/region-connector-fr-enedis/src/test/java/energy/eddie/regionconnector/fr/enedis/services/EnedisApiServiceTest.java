package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.regionconnector.fr.enedis.FrEnedisPostgresqlContainer;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
class EnedisApiServiceTest {
    @ClassRule
    public static PostgreSQLContainer<FrEnedisPostgresqlContainer> postgreSQLContainer = FrEnedisPostgresqlContainer.getInstance();
    @MockBean
    private EnedisApi mockEnedisApi;
    @Autowired
    private EnedisApiService enedisApiService;

    @BeforeAll
    static void beforeAll() {
        postgreSQLContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("region-connector.fr.enedis.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("region-connector.fr.enedis.datasource.username", postgreSQLContainer::getUsername);
        registry.add("region-connector.fr.enedis.datasource.password", postgreSQLContainer::getPassword);
    }

    @Test
    void testGetConsumptionLoadCurve_withRetry() throws ApiException {
        //Given
        String usagePointId = "123";
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        ConsumptionLoadCurveMeterReading mockedRecord = new ConsumptionLoadCurveMeterReading();
        when(mockEnedisApi.getConsumptionLoadCurve(usagePointId, start, end))
                .thenThrow(new ApiException())
                .thenReturn(mockedRecord);

        //When
        ConsumptionLoadCurveMeterReading result = enedisApiService.getConsumptionLoadCurve(usagePointId, start, end);

        //Then
        assertEquals(mockedRecord, result);
    }
}