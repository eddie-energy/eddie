package energy.eddie.regionconnector.es.datadis.client;

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

class NettyDataApiClientTest {

    DataApi dataApi = new NettyDataApiClient(
            HttpClient.create(),
            new MyTokenProvider(), new DatadisEndpoints());

    @Test
    @Disabled("Integration test, that needs real credentials")
    void getSupplies_withAuthorizedNif_returnsSupplies() {
        StepVerifier.create(dataApi.getSupplies("replace_me", null))
                .expectNextMatches(supplies -> {
                    System.out.println(Arrays.toString(supplies.toArray()));
                    return supplies.size() > 0;
                })
                .verifyComplete();

    }

    @Test
    @Disabled("Integration test, that needs real credentials")
    void getConsumptionKwh_withAuthorizedNif_returnsConsumptionKwh() {
        var month = LocalDate.of(2023, 5, 1);
        MeteringDataRequest request = new MeteringDataRequest("replace_me", "replace_me", "5", month, month, MeasurementType.HOURLY, "4");
        System.out.println(request);
        StepVerifier.create(dataApi.getConsumptionKwh(request))
                .expectNextMatches(consumptionKwhs -> {
                    System.out.println(Arrays.toString(consumptionKwhs.toArray()));
                    return consumptionKwhs.size() > 0;
                })
                .verifyComplete();

    }

    static class MyTokenProvider implements DatadisTokenProvider {

        @Override
        public Mono<String> getToken() {
            return Mono.just(
                    "replace_me");
        }
    }
}