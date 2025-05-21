package energy.eddie.core.services.v0_91_08;

import energy.eddie.api.v0_91_08.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v0_91_08.VHDEnvelope;
import energy.eddie.core.converters.v0_91_08.MeasurementConverterCIM_v0_91_08;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ValidatedHistoricalDataMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedHistoricalDataMarketDocumentService.class);

    private final Sinks.Many<VHDEnvelope> consumptionRecordSink = Sinks.many()
                                                                       .multicast()
                                                                       .onBackpressureBuffer();
    private final MeasurementConverterCIM_v0_91_08 converter;

    public ValidatedHistoricalDataMarketDocumentService(MeasurementConverterCIM_v0_91_08 converter) {this.converter = converter;}

    public void registerProvider(ValidatedHistoricalDataMarketDocumentProvider provider) {
        LOGGER.info("Registering {}", provider.getClass().getName());
        provider.getValidatedHistoricalDataMarketDocumentsStream()
                .subscribe(consumptionRecordSink::tryEmitNext, consumptionRecordSink::tryEmitError);
    }

    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentStream() {
        return consumptionRecordSink.asFlux()
                                    .map(converter::convert);
    }
}