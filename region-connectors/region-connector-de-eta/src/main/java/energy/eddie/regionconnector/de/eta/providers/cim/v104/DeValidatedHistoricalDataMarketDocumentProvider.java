package energy.eddie.regionconnector.de.eta.providers.cim.v104;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.regionconnector.de.eta.client.EtaPlusApiClient;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.providers.cim.EtaToCimMapper;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DeValidatedHistoricalDataMarketDocumentProvider implements ValidatedHistoricalDataMarketDocumentProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeValidatedHistoricalDataMarketDocumentProvider.class);

    private final Sinks.Many<VHDEnvelope> documentSink = Sinks.many().multicast().onBackpressureBuffer();

    private final EtaPlusApiClient apiClient;
    private final EtaToCimMapper mapper;
    private final DePermissionRequestRepository repository;
    private final ValidatedHistoricalDataStream stream;

    public DeValidatedHistoricalDataMarketDocumentProvider(
            EtaPlusApiClient apiClient,
            EtaToCimMapper mapper,
            DePermissionRequestRepository repository,
            ValidatedHistoricalDataStream stream
    ) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.repository = repository;
        this.stream = stream;
    }

    @Override
    public Flux<VHDEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return documentSink.asFlux();
    }

    public void emitDocument(VHDEnvelope document) {
        documentSink.tryEmitNext(document);
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void fetchAndEmitData() {
        List<DePermissionRequest> activeRequests = repository.findByStatus(PermissionProcessStatus.ACCEPTED);

        for (DePermissionRequest request : activeRequests) {
            this.apiClient.streamMeteredData(request.meteringPointId(), request.start(), LocalDate.now(ZoneId.of("UTC")))
                    .collectList()
                    .subscribe((List<EtaPlusMeteredData.MeterReading> readings) -> {
                        if (!readings.isEmpty()) {

                            EtaPlusMeteredData rawData = new EtaPlusMeteredData(
                                    request.meteringPointId(),
                                    request.start(),
                                    request.end(),
                                    readings,
                                    ""
                            );
                            try {
                                stream.publish(request, rawData);
                            } catch (Exception e) {
                                LOGGER.error("Failed to publish to stream", e);
                            }

                            mapper.mapToEnvelope(request, readings)
                                    .ifPresent(this::emitDocument);
                        }
                    });
        }
    }
}