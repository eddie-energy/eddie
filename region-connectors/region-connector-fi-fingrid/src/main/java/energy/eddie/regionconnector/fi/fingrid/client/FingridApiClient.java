package energy.eddie.regionconnector.fi.fingrid.client;

import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.regionconnector.fi.fingrid.config.FingridConfiguration;
import jakarta.annotation.Nullable;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

@Component
public class FingridApiClient {
    private final WebClient webClient;
    private final FingridConfiguration configuration;
    private Health health = Health.unknown().build();

    public FingridApiClient(FingridConfiguration configuration, WebClient webClient) {
        this.webClient = webClient;
        this.configuration = configuration;
    }

    public Mono<TimeSeriesResponse> getTimeSeriesData(
            String meteringPointEAN,
            String customerIdentification,
            ZonedDateTime start,
            ZonedDateTime end,
            @Nullable String resolutionDuration,
            @Nullable EnergyProductTypeList productType
    ) {
        if (isNotActiveOrReactiveEnergy(productType)) {
            throw new IllegalArgumentException("product type has to be active or reactive, not %s".formatted(productType));
        }
        return webClient.get()
                        .uri(
                                "GetTimeSeriesData",
                                uriBuilder -> uriBuilder
                                        .queryParam("organisationUser", configuration.organisationUser())
                                        .queryParam("meteringPointEAN", meteringPointEAN)
                                        .queryParam("customerIdentification", customerIdentification)
                                        .queryParam("periodStartTS", start.toString())
                                        .queryParam("periodEndTS", end.toString())
                                        .queryParam("productType", productType == null ? null : productType.value())
                                        .queryParam("resolutionDuration", resolutionDuration)
                                        .build()
                        )
                        .retrieve()
                        .bodyToMono(TimeSeriesResponse.class)
                        .doOnError(Exception.class, err -> health = Health.down(err).build())
                        .doOnSuccess(ignored -> health = Health.up().build());
    }

    private static boolean isNotActiveOrReactiveEnergy(@Nullable EnergyProductTypeList productType) {
        return productType != null
               && (productType != EnergyProductTypeList.ACTIVE_ENERGY && productType != EnergyProductTypeList.REACTIVE_ENERGY);
    }

    public Health health() {
        return health;
    }
}
