package energy.eddie.api.agnostic.retransmission;

import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import reactor.core.publisher.Flux;

public interface RetransmissionRequestRouter {
    /**
     * Returns a stream of retransmission results.
     * Each {@link RetransmissionRequest} should result in a {@link RetransmissionResult} being emitted.
     */
    Flux<RetransmissionResult> retransmissionResults();
}
