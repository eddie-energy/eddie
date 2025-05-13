package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.openapi.model.*;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class CustomerDataClient {
    private final WebClient webClient;

    public CustomerDataClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<AccountsEndpoint200ResponseAllOfAccountsInner>> accounts(
            URI accountsEndpoint,
            OAuthCredentials credentials
    ) {
        return getAll(AccountsEndpoint200Response.class,
                      accountsEndpoint,
                      AccountsEndpoint200Response::getNext,
                      credentials)
                .flatMapIterable(AccountsEndpoint200Response::getAccounts)
                .collectList();
    }

    public Mono<List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner>> serviceContracts(
            URI serviceContractsEndpoint,
            OAuthCredentials credentials
    ) {
        return getAll(ServiceContractEndpoint200Response.class,
                      serviceContractsEndpoint,
                      ServiceContractEndpoint200Response::getNext,
                      credentials)
                .flatMapIterable(ServiceContractEndpoint200Response::getServiceContracts)
                .collectList();
    }

    public Mono<List<ServicePointEndpoint200ResponseAllOfServicePointsInner>> servicePoints(
            URI servicePointsEndpoint,
            OAuthCredentials credentials
    ) {
        return getAll(
                ServicePointEndpoint200Response.class, servicePointsEndpoint, ServicePointEndpoint200Response::getNext,
                credentials
        )
                .flatMapIterable(ServicePointEndpoint200Response::getServicePoints)
                .collectList();
    }

    public Mono<List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner>> meterDevices(
            URI meterDeviceEndpoint,
            OAuthCredentials credentials
    ) {
        return getAll(MeterDeviceEndpoint200Response.class,
                      meterDeviceEndpoint,
                      MeterDeviceEndpoint200Response::getNext,
                      credentials)
                .flatMapIterable(MeterDeviceEndpoint200Response::getMeterDevices)
                .collectList();
    }

    /**
     * Retrieves all usage segments between after and before.
     *
     * @param before                Filters for all usage segments before this datetime {@code before > usagesegment.datetime}
     * @param after                 Filters for all usage segments after this datetime {@code after < usagesegment.datetime}
     * @param usageSegmentsEndpoint The API endpoint for usage segments
     * @param credentials           Credentials of the final customer
     * @return all usage segments between after and before
     */
    public Mono<List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>> usageSegments(
            ZonedDateTime before,
            ZonedDateTime after,
            URI usageSegmentsEndpoint,
            OAuthCredentials credentials
    ) {
        return usageSegments(credentials, before, after, usageSegmentsEndpoint)
                .flatMapIterable(UsageSegmentEndpoint200Response::getUsageSegments)
                .collectList();
    }

    private Flux<UsageSegmentEndpoint200Response> usageSegments(
            OAuthCredentials credentials,
            ZonedDateTime before,
            ZonedDateTime after,
            URI usageSegmentsEndpoint
    ) {
        var page = webClient.get()
                            .uri(usageSegmentsEndpoint.toString(),
                                 uriBuilder -> uriBuilder.queryParam("before", before)
                                                         .queryParam("after", after)
                                                         .build())
                            .headers(setBearerToken(credentials))
                            .retrieve()
                            .bodyToMono(UsageSegmentEndpoint200Response.class);
        return expand(page, credentials, UsageSegmentEndpoint200Response::getNext, this::usageSegments);
    }

    private Flux<UsageSegmentEndpoint200Response> usageSegments(URI next, OAuthCredentials credentials) {
        var page = webClient.get()
                            .uri(next)
                            .headers(setBearerToken(credentials))
                            .retrieve()
                            .bodyToMono(UsageSegmentEndpoint200Response.class);
        return expand(page, credentials, UsageSegmentEndpoint200Response::getNext, this::usageSegments);
    }

    private <T> Flux<T> getAll(Class<T> clazz, URI uri, Function<T, URI> next, OAuthCredentials credentials) {
        var response = webClient.get()
                                .uri(uri)
                                .headers(setBearerToken(credentials))
                                .retrieve()
                                .bodyToMono(clazz);
        return response
                .expand(res -> {
                    var next1 = next.apply(res);
                    if (next1 == null) {
                        return Mono.empty();
                    }
                    return getAll(clazz, uri, next, credentials);
                });
    }

    private <T> Flux<T> expand(
            Mono<T> response,
            OAuthCredentials credentials,
            Function<T, URI> getNext,
            BiFunction<URI, OAuthCredentials, Publisher<T>> request
    ) {
        return response
                .expand(res -> {
                    var next = getNext.apply(res);
                    if (next == null) {
                        return Mono.empty();
                    }
                    return request.apply(next, credentials);
                });
    }

    @SuppressWarnings("DataFlowIssue")
    private static Consumer<HttpHeaders> setBearerToken(OAuthCredentials token) {
        return h -> h.setBearerAuth(token.accessToken());
    }
}
