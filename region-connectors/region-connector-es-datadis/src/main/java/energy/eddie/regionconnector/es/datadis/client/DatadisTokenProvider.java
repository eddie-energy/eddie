package energy.eddie.regionconnector.es.datadis.client;

import reactor.core.publisher.Mono;

public interface DatadisTokenProvider {
    Mono<String> getToken();
}
