package energy.eddie.api.v0_82;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link ValidatedHistoricalDataEnvelope}s available.
 */
public interface ValidatedHistoricalDataEnvelopeProvider extends AutoCloseable {
    /**
     * Data stream of all ValidatedHistoricalDataEnvelopes created by this region connector.
     *
     * @return ValidatedHistoricalDataEnvelope stream
     */
    Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream();

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    void close() throws Exception;
}