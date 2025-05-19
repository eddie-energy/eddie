package energy.eddie.api.v0_91_08;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.cim.v0_91_08.VHDEnvelope;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link VHDEnvelope}s available.
 */
public interface ValidatedHistoricalDataMarketDocumentProvider {
    /**
     * Data stream of all ValidatedHistoricalDataEnvelopes created by this region connector.
     *
     * @return ValidatedHistoricalDataEnvelope stream
     */
    Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream();
}
