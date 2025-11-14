package energy.eddie.core.services.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.core.converters.MeasurementConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ValidatedHistoricalDataEnvelopeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataEnvelopeService.class);

    private final Sinks.Many<ValidatedHistoricalDataEnvelope> consumptionRecordSink = Sinks.many()
                                                                                           .multicast()
                                                                                           .onBackpressureBuffer();
    private final MeasurementConverter converter;

    public ValidatedHistoricalDataEnvelopeService(MeasurementConverter converter) {this.converter = converter;}

    public void registerProvider(ValidatedHistoricalDataEnvelopeProvider validatedHistoricalDataEnvelopeProvider) {
        LOGGER.info("EddieValidatedHistoricalDataMarketDocumentService: Registering {}",
                    validatedHistoricalDataEnvelopeProvider.getClass().getName());
        validatedHistoricalDataEnvelopeProvider.getValidatedHistoricalDataMarketDocumentsStream()
                                               .onErrorContinue((err, obj) -> LOGGER.warn(
                                                       "Encountered error while processing validated historical data market document",
                                                       err))
                                               .subscribe(consumptionRecordSink::tryEmitNext);
    }

    public Flux<ValidatedHistoricalDataEnvelope> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return consumptionRecordSink.asFlux()
                                    .map(this::applyConverter);
    }

    private ValidatedHistoricalDataEnvelope applyConverter(ValidatedHistoricalDataEnvelope vhd) {
        return converter.convert(vhd);
    }
}