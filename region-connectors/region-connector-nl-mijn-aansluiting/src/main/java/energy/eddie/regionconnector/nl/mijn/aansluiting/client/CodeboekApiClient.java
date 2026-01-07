package energy.eddie.regionconnector.nl.mijn.aansluiting.client;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoint;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MeteringPoints;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CodeboekApiClient {
    private static final MeteringPoint.ProductEnum[] ALLOWED_PRODUCTS = {MeteringPoint.ProductEnum.ELK, MeteringPoint.ProductEnum.GAS};
    private final WebClient webClient;
    private Health health = Health.unknown().build();

    public CodeboekApiClient(MijnAansluitingConfiguration config, WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl(config.codeboekApi().toString())
                .defaultHeader("X-API-Key", config.codeboekApiToken())
                .build();
    }

    public Flux<MeteringPoints> meteringPoints(String postalCode, String streetNumber) {
        return Flux.fromArray(ALLOWED_PRODUCTS)
                      .flatMap(product -> meteringPoints(postalCode, streetNumber, product));
    }

    public Mono<MeteringPoints> meteringPoints(
            String postalCode,
            String streetNumber,
            MeteringPoint.ProductEnum product
    ) {
        return webClient.get()
                        .uri(builder -> builder.queryParam("product", product.getValue())
                                               .queryParam("streetNumber", streetNumber)
                                               .queryParam("postalCode", postalCode)
                                               .build())
                        .retrieve()
                        .bodyToMono(MeteringPoints.class)
                        .doOnSuccess(res -> health = Health.up().build())
                        .doOnError(CodeboekApiClient::isServiceUnavailableOrInternalServerError,
                                   error -> health = Health.down(error).build());
    }

    public Health health() {
        return health;
    }

    private static boolean isServiceUnavailableOrInternalServerError(Throwable error) {
        return error instanceof WebClientResponseException.ServiceUnavailable || error instanceof WebClientResponseException.InternalServerError;
    }
}
