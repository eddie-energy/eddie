package energy.eddie.regionconnector.es.datadis.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.es.datadis.DatadisSpringConfig;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.SupplyApi;
import energy.eddie.regionconnector.es.datadis.config.PlainDatadisConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.util.Arrays;

class NettySupplyApiClientIntegrationTest {
    private final ObjectMapper mapper = new DatadisSpringConfig().objectMapper();

    SupplyApi supplyApi = new NettySupplyApiClient(
            HttpClient.create(),
            mapper,
            () -> Mono.just("replace_me"),
            new PlainDatadisConfiguration("username", "password", "https://datadis.es")
    );

    @Test
    @Disabled("Integration test, that needs real credentials")
    void getSupplies_withAuthorizedNif_returnsSupplies() {
        StepVerifier.create(supplyApi.getSupplies("replace_me", null))
                    .expectNextMatches(supplies -> {
                        System.out.println(Arrays.toString(supplies.toArray()));
                        return !supplies.isEmpty();
                    })
                    .verifyComplete();
    }

    @Test
    @Disabled("Integration test, that needs real credentials")
    void getSupplies_withUnauthorizedNif_returnsUnauthorizedException() {
        StepVerifier.create(supplyApi.getSupplies("replace_me", null))
                    .expectErrorMatches(throwable -> throwable instanceof DatadisApiException datadisApiException
                                                     && datadisApiException.statusCode() == HttpStatus.UNAUTHORIZED.value())
                    .verify();
    }
}