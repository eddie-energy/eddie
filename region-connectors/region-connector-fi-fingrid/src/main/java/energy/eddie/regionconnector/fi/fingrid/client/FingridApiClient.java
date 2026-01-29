package energy.eddie.regionconnector.fi.fingrid.client;

import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerDataResponse;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.config.FingridConfiguration;
import jakarta.annotation.Nullable;
import org.springframework.boot.health.contributor.Health;
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
                                uriBuilder -> {
                                    uriBuilder = uriBuilder
                                            .queryParam("organisationUser", configuration.organisationUser())
                                            .queryParam("meteringPointEAN", meteringPointEAN)
                                            .queryParam("customerIdentification", customerIdentification)
                                            .queryParam("periodStartTS", start.toString())
                                            .queryParam("periodEndTS", end.toString());
                                    if (productType != null) {
                                        uriBuilder = uriBuilder.queryParam("productType", productType.value());
                                    }
                                    if (resolutionDuration != null) {
                                        uriBuilder = uriBuilder.queryParam("resolutionDuration", resolutionDuration);
                                    }
                                    return uriBuilder.build();
                                }
                        )
                        .retrieve()
                        .bodyToMono(TimeSeriesResponse.class)
                        .doOnError(err -> health = Health.down(err).build())
                        .doOnSuccess(ignored -> health = Health.up().build());
    }

    public Mono<CustomerDataResponse> getCustomerData(String customerIdentification) {
        return webClient.get()
                        .uri("GetCustomerData",
                             uriBuilder -> uriBuilder.queryParam("organisationUser", configuration.organisationUser())
                                                     .queryParam("customerIdentification", customerIdentification)
                                                     .build())
                        .retrieve()
                        .bodyToMono(CustomerDataResponse.class)
                        .doOnError(err -> health = Health.down(err).build())
                        .doOnSuccess(ignored -> health = Health.up().build());
    }

    public Health health() {
        return health;
    }

    private static boolean isNotActiveOrReactiveEnergy(@Nullable EnergyProductTypeList productType) {
        return productType != null
               && (productType != EnergyProductTypeList.ACTIVE_ENERGY && productType != EnergyProductTypeList.REACTIVE_ENERGY);
    }
}
