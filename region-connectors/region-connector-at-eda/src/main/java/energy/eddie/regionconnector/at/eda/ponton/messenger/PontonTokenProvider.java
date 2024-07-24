package energy.eddie.regionconnector.at.eda.ponton.messenger;

import reactor.core.publisher.Mono;

public interface PontonTokenProvider {
    Mono<String> getToken();
}
