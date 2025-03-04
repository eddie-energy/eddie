package energy.eddie.api.v0_91_08.outbound;

import energy.eddie.cim.v0_91_08.vhd.VHDEnveloppe;
import reactor.core.publisher.Flux;

/**
 * An outbound connector that takes a stream of {@link energy.eddie.cim.v0_91_08.vhd.VHDEnveloppe}s.
 * @see energy.eddie.api.v0_82.outbound.ValidatedHistoricalDataEnvelopeOutboundConnector
 */
public interface ValidatedHistoricalDataMarketDocumentOutboundConnector {

    void setEddieValidatedHistoricalDataMarketDocumentStream(
            Flux<VHDEnveloppe> marketDocumentStream
    );
}
