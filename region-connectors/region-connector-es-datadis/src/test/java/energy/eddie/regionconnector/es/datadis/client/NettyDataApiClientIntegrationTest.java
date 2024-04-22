package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.DatadisSpringConfig;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Arrays;

class NettyDataApiClientIntegrationTest {
    private final ObjectMapper mapper = new DatadisSpringConfig().objectMapper();

    DataApi dataApi = new NettyDataApiClient(
            HttpClient.create(),
            mapper,
            () -> Mono.just("replace_me"),
            "https://datadis.es");

    @Test
    @Disabled("Integration test, that needs real credentials")
    void getConsumptionKwh_withAuthorizedNif_returnsConsumptionKwh() {
        var month = LocalDate.of(2023, 5, 1);
        MeteringDataRequest request = new MeteringDataRequest("replace_me", "replace_me", "replace_me", month, month, MeasurementType.HOURLY, "replace_me");
        System.out.println(request);
        StepVerifier.create(dataApi.getConsumptionKwh(request))
                .expectNextMatches(consumptionKwhs -> {
                    System.out.println(Arrays.toString(consumptionKwhs.toArray()));
                    return !consumptionKwhs.isEmpty();
                })
                .verifyComplete();
    }
}