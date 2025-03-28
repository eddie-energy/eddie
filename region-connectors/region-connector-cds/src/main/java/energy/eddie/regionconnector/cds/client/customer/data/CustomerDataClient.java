package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.regionconnector.cds.master.data.CdsServer;
import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.services.oauth.CustomerDataTokenService;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class CustomerDataClient {
    private final WebClient webClient;
    private final CdsServer cdsServer;
    private final CustomerDataTokenService tokenService;

    public CustomerDataClient(WebClient webClient, CdsServer cdsServer, CustomerDataTokenService tokenService) {
        this.webClient = webClient;
        this.cdsServer = cdsServer;
        this.tokenService = tokenService;
    }

    public Mono<List<AccountsEndpoint200ResponseAllOfAccountsInner>> accounts(CdsPermissionRequest permissionRequest) {
        var accountsEndpoint = cdsServer.endpoints().accountsEndpoint();
        var request = getAll(AccountsEndpoint200Response.class, accountsEndpoint, AccountsEndpoint200Response::getNext);
        return requestWithToken(permissionRequest, request)
                .flatMapIterable(AccountsEndpoint200Response::getAccounts)
                .collectList();
    }

    public Mono<List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner>> serviceContracts(
            CdsPermissionRequest permissionRequest
    ) {
        var uri = cdsServer.endpoints().serviceContractsEndpoint();
        var request = getAll(ServiceContractEndpoint200Response.class,
                             uri,
                             ServiceContractEndpoint200Response::getNext);
        return requestWithToken(permissionRequest, request)
                .flatMapIterable(ServiceContractEndpoint200Response::getServiceContracts)
                .collectList();
    }

    public Mono<List<ServicePointEndpoint200ResponseAllOfServicePointsInner>> servicePoints(CdsPermissionRequest permissionRequest) {
        var uri = cdsServer.endpoints().servicePointsEndpoint();
        var request = getAll(ServicePointEndpoint200Response.class, uri, ServicePointEndpoint200Response::getNext);
        return requestWithToken(permissionRequest, request)
                .flatMapIterable(ServicePointEndpoint200Response::getServicePoints)
                .collectList();
    }

    public Mono<List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner>> meterDevices(CdsPermissionRequest permissionRequest) {
        var uri = cdsServer.endpoints().meterDeviceEndpoint();
        var request = getAll(MeterDeviceEndpoint200Response.class, uri, MeterDeviceEndpoint200Response::getNext);
        return requestWithToken(permissionRequest, request)
                .flatMapIterable(MeterDeviceEndpoint200Response::getMeterDevices)
                .collectList();
    }

    public Mono<List<BillSectionEndpoint200ResponseAllOfBillSectionsInner>> billSections(CdsPermissionRequest permissionRequest) {
        var uri = cdsServer.endpoints().billSectionEndpoint();
        var request = getAll(BillSectionEndpoint200Response.class, uri, BillSectionEndpoint200Response::getNext);
        return requestWithToken(permissionRequest, request)
                .flatMapIterable(BillSectionEndpoint200Response::getBillSections)
                .collectList();
    }

    /**
     * Retrieves all usage segments between after and before.
     *
     * @param permissionRequest permission request that the data is queried for
     * @param before            Filters for all usage segments before this datetime {@code before > usagesegment.datetime}
     * @param after             Filters for all usage segments after this datetime {@code after < usagesegment.datetime}
     * @return all usage segments
     */
    public Mono<List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>> usagePoints(
            CdsPermissionRequest permissionRequest,
            ZonedDateTime before,
            ZonedDateTime after
    ) {
        Function<OAuthCredentials, Publisher<UsageSegmentEndpoint200Response>> req =
                token -> usagePoints(token, before, after);
        return requestWithToken(permissionRequest, req)
                .flatMapIterable(UsageSegmentEndpoint200Response::getUsageSegments)
                .collectList();
    }

    private Flux<UsageSegmentEndpoint200Response> usagePoints(
            OAuthCredentials credentials,
            ZonedDateTime before,
            ZonedDateTime after
    ) {
        var page = webClient.get()
                            .uri(cdsServer.endpoints().usagePointEndpoint().toString(),
                                 uriBuilder -> uriBuilder.queryParam("before", before)
                                                         .queryParam("after", after)
                                                         .build())
                            .headers(setBearerToken(credentials))
                            .retrieve()
                            .bodyToMono(UsageSegmentEndpoint200Response.class);
        return expand(page, credentials, UsageSegmentEndpoint200Response::getNext, this::usagePoints);
    }

    private Flux<UsageSegmentEndpoint200Response> usagePoints(URI next, OAuthCredentials credentials) {
        var page = webClient.get()
                            .uri(next)
                            .headers(setBearerToken(credentials))
                            .retrieve()
                            .bodyToMono(UsageSegmentEndpoint200Response.class);
        return expand(page, credentials, UsageSegmentEndpoint200Response::getNext, this::usagePoints);
    }

    private <T> Function<OAuthCredentials, Publisher<T>> getAll(Class<T> clazz, URI uri, Function<T, URI> next) {
        return token -> getAll(clazz, uri, next, token);
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


    private <T> Flux<T> requestWithToken(
            CdsPermissionRequest permissionRequest,
            Function<OAuthCredentials, Publisher<T>> request
    ) {
        return tokenService.getOAuthCredentialsAsync(permissionRequest.permissionId(), cdsServer)
                           .flatMapMany(request);
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
