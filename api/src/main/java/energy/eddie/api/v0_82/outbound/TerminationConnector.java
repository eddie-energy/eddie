package energy.eddie.api.v0_82.outbound;

import energy.eddie.api.utils.Pair;
import energy.eddie.cim.v0_82.pmd.PermissionEnveloppe;
import reactor.core.publisher.Flux;

public interface TerminationConnector {
    Flux<Pair<String, PermissionEnveloppe>> getTerminationMessages();

}
