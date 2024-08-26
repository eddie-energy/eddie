package energy.eddie.api.v0_82;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnveloppe;
import reactor.core.publisher.Flux;

/**
 * Used to extend a {@link RegionConnector} by making a Flux of
 * {@link ValidatedHistoricalDataEnveloppe}s available.
 */
public interface ValidatedHistoricalDataEnveloppeProvider extends AutoCloseable {
    /**
     * Data stream of all ValidatedHistoricalDataEnveloppes created by this region connector.
     *
     * @return ValidatedHistoricalDataEnveloppe stream
     */
    Flux<ValidatedHistoricalDataEnveloppe> getValidatedHistoricalDataMarketDocumentsStream();

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    void close() throws Exception;
}