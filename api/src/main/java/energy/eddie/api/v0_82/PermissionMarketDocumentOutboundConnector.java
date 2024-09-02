package energy.eddie.api.v0_82;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import reactor.core.publisher.Flux;

public interface PermissionMarketDocumentOutboundConnector {
    void setPermissionMarketDocumentStream(Flux<PermissionEnvelope> permissionMarketDocumentStream);
}
