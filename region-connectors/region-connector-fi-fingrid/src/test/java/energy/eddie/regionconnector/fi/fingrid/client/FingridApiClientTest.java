package energy.eddie.regionconnector.fi.fingrid.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.regionconnector.fi.fingrid.config.FingridConfiguration;
import energy.eddie.regionconnector.shared.utils.ObjectMapperConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class FingridApiClientTest {
    public static MockWebServer mockBackEnd = new MockWebServer();
    public static ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    @SuppressWarnings("unused")
    @Spy
    private final FingridConfiguration config = new FingridConfiguration("orgUser", "orgName", "http://localhost");
    @SuppressWarnings("unused")
    @Spy
    private final WebClient webClient = WebClient.builder()
                                                 .baseUrl("http://localhost:" + mockBackEnd.getPort())
                                                 .build();
    @InjectMocks
    private FingridApiClient apiClient;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }


    @Test
    void getTimeSeriesDataThrowsOnInvalidProductType() {
        // Given
        var productType = EnergyProductTypeList.WATER;
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        var end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1);

        // When & Then
        //noinspection ReactiveStreamsUnusedPublisher
        assertThrows(IllegalArgumentException.class,
                     () -> apiClient.getTimeSeriesData("mid", "cid", start, end, null, productType));
    }

    @ParameterizedTest
    @EnumSource(names = {"ACTIVE_ENERGY", "REACTIVE_ENERGY"})
    void getTimeSeriesDataDoesNotThrow(EnergyProductTypeList productType) throws JsonProcessingException {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var start = now.minusDays(10);
        var end = now.minusDays(1);
        var resp = timeSeriesResponse(now);
        mockBackEnd.enqueue(
                new MockResponse()
                        .setBody(objectMapper.writeValueAsString(resp))
                        .addHeader("Content-Type", "application/json")
        );

        // When
        var res = apiClient.getTimeSeriesData("mid", "cid", start, end, null, productType);

        // Then
        StepVerifier.create(res)
                    .assertNext(content -> assertEquals(resp, content))
                    .verifyComplete();
    }

    private static TimeSeriesResponse timeSeriesResponse(ZonedDateTime now) {
        var sender = new Party("sender");
        var receiver = new Party("receiver");
        var header = new Header("id",
                                sender,
                                sender,
                                receiver,
                                receiver,
                                "role",
                                now,
                                "meter-reading",
                                "org-user");
        return new TimeSeriesResponse(
                new TimeSeriesData(
                        header,
                        new Transaction(null, null, List.of())
                )
        );
    }


    @Test
    void health_returnsUnknown() {
        // Given
        // When
        Health res = apiClient.health();

        // Then
        assertEquals(Health.unknown().build(), res);
    }
}