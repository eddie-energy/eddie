package energy.eddie.regionconnector.fr.enedis.providers.agnostic;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Flow;

@Component
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class EnedisRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnedisRawDataProvider.class);
    private final Flux<IdentifiableMeterReading> identifiableMeterReadingFlux;
    private final PermissionRequestRepository<FrEnedisPermissionRequest> repository;

    public EnedisRawDataProvider(Flux<IdentifiableMeterReading> identifiableMeterReadingFlux,
                                 PermissionRequestRepository<FrEnedisPermissionRequest> repository
    ) {
        this.identifiableMeterReadingFlux = identifiableMeterReadingFlux;
        this.repository = repository;
    }

    @Override
    public Flow.Publisher<RawDataMessage> getRawDataStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(
                identifiableMeterReadingFlux
                        .map(this::createRawDataMessage));
    }

    private RawDataMessage createRawDataMessage(IdentifiableMeterReading meterReading) {
        var permissionRequest = repository.findByPermissionId(meterReading.permissionId());
        DataSourceInformation dataSourceInfo = null;

        if (permissionRequest.isEmpty())
            LOGGER.error("No permission with ID {} found in repository.", meterReading.permissionId());
        else {
            dataSourceInfo = permissionRequest.get().dataSourceInformation();
        }

        return new RawDataMessage(meterReading.permissionId(), meterReading.connectionId(), meterReading.dataNeedId(),
                dataSourceInfo, ZonedDateTime.now(ZoneId.of("UTC")), meterReading.payload().toString());
    }

    @Override
    public void close() {
        // complete is emitted when identifiableMeterReadingFlux completes
    }
}