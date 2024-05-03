package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class EnerginetRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetRawDataProvider.class);
    private final Flux<IdentifiableApiResponse> identifiableApiResponseFlux;
    private final DkPermissionRequestRepository repository;

    public EnerginetRawDataProvider(
            Flux<IdentifiableApiResponse> identifiableApiResponseFlux,
            DkPermissionRequestRepository repository
    ) {
        this.identifiableApiResponseFlux = identifiableApiResponseFlux;
        this.repository = repository;
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return identifiableApiResponseFlux
                .map(this::createRawDataMessage);
    }

    private RawDataMessage createRawDataMessage(IdentifiableApiResponse response) {
        String permissionId = response.permissionRequest().permissionId();
        var permissionRequest = repository.findByPermissionId(permissionId);
        DataSourceInformation dataSourceInfo = null;

        if (permissionRequest.isEmpty()) {
            LOGGER.error("No permission with ID {} found in repository.", permissionId);
        } else {
            dataSourceInfo = permissionRequest.get().dataSourceInformation();
        }

        var rawString = response.apiResponse().toString();

        return new RawDataMessage(permissionId, response.permissionRequest().connectionId(), response.permissionRequest().dataNeedId(),
                dataSourceInfo, ZonedDateTime.now(ZoneId.of("UTC")), rawString);
    }

    @Override
    public void close() {
        // complete is emitted when identifiableApiResponseFlux completes
    }
}
