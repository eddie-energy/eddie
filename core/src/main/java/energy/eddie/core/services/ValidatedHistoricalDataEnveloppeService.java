package energy.eddie.core.services;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnveloppeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnveloppe;
import energy.eddie.core.converters.MeasurementConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ValidatedHistoricalDataEnveloppeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataEnveloppeService.class);

    private final Sinks.Many<ValidatedHistoricalDataEnveloppe> consumptionRecordSink = Sinks.many()
                                                                                                      .multicast()
                                                                                                      .onBackpressureBuffer();
    private final MeasurementConverter converter;

    public ValidatedHistoricalDataEnveloppeService(MeasurementConverter converter) {this.converter = converter;}

    public void registerProvider(ValidatedHistoricalDataEnveloppeProvider validatedHistoricalDataEnveloppeProvider) {
        LOGGER.info("EddieValidatedHistoricalDataMarketDocumentService: Registering {}",
                    validatedHistoricalDataEnveloppeProvider.getClass().getName());
        validatedHistoricalDataEnveloppeProvider.getValidatedHistoricalDataMarketDocumentsStream()
                                                .doOnNext(consumptionRecordSink::tryEmitNext)
                                                .doOnError(consumptionRecordSink::tryEmitError)
                                                .subscribe();
    }

    public Flux<ValidatedHistoricalDataEnveloppe> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return consumptionRecordSink.asFlux()
                                    .map(this::applyConverter);
    }

    private ValidatedHistoricalDataEnveloppe applyConverter(ValidatedHistoricalDataEnveloppe vhd) {
        return converter.convert(vhd);
    }
}