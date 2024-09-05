package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.MeteringDataProvider;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NettyDataApiClientTest {

    private static MockWebServer mockBackEnd;
    private static DatadisConfig config;
    private final ObjectMapper mapper = MeteringDataProvider.objectMapper;

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var basePath = "http://localhost:" + mockBackEnd.getPort();
        config = new PlainDatadisConfiguration("username", "password", basePath);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getConsumptionKwh_withWhenReceivingSupplies_returnsSupplies() throws IOException {
        DataApi uut = new NettyDataApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("token"),
                config
        );

        String body = mapper.writeValueAsString(MeteringDataProvider.loadMeteringData());

        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(HttpStatus.OK.value())
                                    .setBody(body));

        StepVerifier.create(uut.getConsumptionKwh(createMeteringDataRequest()))
                    .assertNext(meteringDataList -> assertEquals(744, meteringDataList.size()))
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    private static MeteringDataRequest createMeteringDataRequest() {
        LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
        EsPermissionRequest permissionRequest = mock(EsPermissionRequest.class);
        when(permissionRequest.distributorCode()).thenReturn(Optional.of(DistributorCode.ASEME));
        when(permissionRequest.pointType()).thenReturn(Optional.of(1));
        when(permissionRequest.measurementType()).thenReturn(MeasurementType.HOURLY);
        return MeteringDataRequest.fromPermissionRequest(permissionRequest, now, now);
    }

    @Test
    void getConsumptionKwh_whenReceivingNotFound_producesDatadisApiException() {
        DataApi uut = new NettyDataApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("token"),
                config
        );

        MeteringDataRequest request = createMeteringDataRequest();

        // this would happen e.g. ig we request data with an invalid MeasurementType and PointType combination
        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(HttpStatus.NOT_FOUND.value()));

        StepVerifier.create(uut.getConsumptionKwh(request))
                    .expectError(DatadisApiException.class)
                    .verify(Duration.ofSeconds(2));
    }
}
