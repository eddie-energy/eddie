package energy.eddie.core.services.v1_04;

import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.core.converters.v1_04.MeasurementConverterV1_04CIM;
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
    private final MeasurementConverterV1_04CIM converter;

    public ValidatedHistoricalDataMarketDocumentService(MeasurementConverterV1_04CIM converter) {this.converter = converter;}

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