package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.ContractDetailsProvider;
import energy.eddie.regionconnector.es.datadis.DatadisBeanConfig;
import energy.eddie.regionconnector.es.datadis.api.ContractApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import energy.eddie.regionconnector.es.datadis.dtos.ContractDetails;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NettyContractApiClientTest {
    private static MockWebServer mockBackEnd;
    private static DatadisConfig datadisConfig;
    private final ObjectMapper mapper = new DatadisBeanConfig().objectMapper();

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var basePath = "http://localhost:" + mockBackEnd.getPort();
        datadisConfig = new PlainDatadisConfiguration("username", "password", basePath);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getContractDetails_withWhenReceivingContractDetails_returnsContractDetails() throws IOException {
        // Given
        ContractApi uut = new NettyContractApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("token"),
                datadisConfig
        );

        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(HttpStatus.OK.value())
                                    .setBody(ContractDetailsProvider.loadRawContractDetails()));

        // When
        Mono<List<ContractDetails>> contractDetails = uut.getContractDetails("authorizedNif",
                                                                             DistributorCode.IDE.getCode(),
                                                                             "ES00XXXXXXXXXXXXXXXX");

        // Then
        StepVerifier.create(contractDetails)
                    .assertNext(meteringDataList -> assertAll(
                            () -> assertEquals(1, meteringDataList.size()),
                            () -> assertEquals("ES00XXXXXXXXXXXXXXXX", meteringDataList.getFirst().cups()),
                            () -> assertTrue(meteringDataList.getFirst().installedCapacity().isEmpty()),
                            () -> assertEquals(LocalDate.of(2023, 7, 22), meteringDataList.getFirst().startDate()),
                            () -> assertTrue(meteringDataList.getFirst().endDate().isEmpty()),
                            () -> assertEquals("3T", meteringDataList.getFirst().codeFare())

                    ))
                    .expectComplete()
                    .verify(Duration.ofSeconds(2));
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"NOT_FOUND", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "TOO_MANY_REQUESTS", "INTERNAL_SERVER_ERROR"})
    void getContractDetails_whenReceivingErrors_producesDatadisApiException(HttpStatus status) {
        // Given
        ContractApi uut = new NettyContractApiClient(
                HttpClient.create(),
                mapper,
                () -> Mono.just("token"),
                datadisConfig
        );


        // this would happen e.g. ig we request data with an invalid MeasurementType and PointType combination
        mockBackEnd.enqueue(new MockResponse()
                                    .setResponseCode(status.value()));

        // When
        Mono<List<ContractDetails>> contractDetails = uut.getContractDetails("authorizedNif",
                                                                             DistributorCode.IDE.getCode(),
                                                                             "ES00XXXXXXXXXXXXXXXX");
        // Then
        StepVerifier.create(contractDetails)
                    .expectError(DatadisApiException.class)
                    .verify(Duration.ofSeconds(2));
    }
}
