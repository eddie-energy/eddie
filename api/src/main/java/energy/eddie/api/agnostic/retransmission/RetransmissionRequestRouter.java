package energy.eddie.api.agnostic.retransmission;

import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface RetransmissionRequestRouter {
    Mono<RetransmissionResult> routeRetransmissionRequest(
            String regionConnectorId,
            RetransmissionRequest retransmissionRequest
    );
}
