package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * CIM v0.82 Validated Historical Data provider for the German ETA Plus connector (backwards compatible).
 */
@Component
public class DeValidatedHistoricalDataEnvelopeProvider {

    private final Flux<IdentifiableValidatedHistoricalData> identifiableData;
    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration deConfiguration;

    public DeValidatedHistoricalDataEnvelopeProvider(
            ValidatedHistoricalDataStream stream,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration
    ) {
        this.identifiableData = stream.validatedHistoricalData();
        this.cimConfig = cimConfig;
        this.deConfiguration = deConfiguration;
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableData
                .map(data -> new IntermediateValidatedHistoricalDataEnvelope(cimConfig, deConfiguration, data))
                .flatMapIterable(IntermediateValidatedHistoricalDataEnvelope::toVhd);
    }
}