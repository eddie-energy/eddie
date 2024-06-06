package energy.eddie.core.services;

import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.core.converters.MeasurementConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class EddieValidatedHistoricalDataMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(energy.eddie.core.services.EddieValidatedHistoricalDataMarketDocumentService.class);

    private final Sinks.Many<EddieValidatedHistoricalDataMarketDocument> consumptionRecordSink = Sinks.many()
                                                                                                      .multicast()
                                                                                                      .onBackpressureBuffer();
    private final MeasurementConverter converter;

    public EddieValidatedHistoricalDataMarketDocumentService(MeasurementConverter converter) {this.converter = converter;}

    public void registerProvider(EddieValidatedHistoricalDataMarketDocumentProvider eddieValidatedHistoricalDataMarketDocumentProvider) {
        LOGGER.info("EddieValidatedHistoricalDataMarketDocumentService: Registering {}",
                    eddieValidatedHistoricalDataMarketDocumentProvider.getClass().getName());
        eddieValidatedHistoricalDataMarketDocumentProvider.getEddieValidatedHistoricalDataMarketDocumentStream()
                                                          .doOnNext(consumptionRecordSink::tryEmitNext)
                                                          .doOnError(consumptionRecordSink::tryEmitError)
                                                          .subscribe();
    }

    public Flux<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return consumptionRecordSink.asFlux()
                                    .map(this::applyConverter);
    }

    private EddieValidatedHistoricalDataMarketDocument applyConverter(EddieValidatedHistoricalDataMarketDocument vhd) {
        return new EddieValidatedHistoricalDataMarketDocument(
                vhd.connectionId(),
                vhd.permissionId(),
                vhd.dataNeedId(),
                converter.convert(vhd.marketDocument())
        );
    }
}