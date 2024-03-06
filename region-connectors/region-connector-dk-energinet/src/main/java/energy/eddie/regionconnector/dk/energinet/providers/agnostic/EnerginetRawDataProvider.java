package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.permission.request.persistence.DkEnerginetCustomerPermissionRequestRepository;
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
    private final DkEnerginetCustomerPermissionRequestRepository repository;

    public EnerginetRawDataProvider(Flux<IdentifiableApiResponse> identifiableApiResponseFlux, DkEnerginetCustomerPermissionRequestRepository repository) {
        this.identifiableApiResponseFlux = identifiableApiResponseFlux;
        this.repository = repository;
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return identifiableApiResponseFlux
                .map(this::createRawDataMessage);
    }

    private RawDataMessage createRawDataMessage(IdentifiableApiResponse response) {
        var permissionRequest = repository.findByPermissionId(response.permissionId());
        DataSourceInformation dataSourceInfo = null;

        if (permissionRequest.isEmpty())
            LOGGER.error("No permission with ID {} found in repository.", response.permissionId());
        else {
            dataSourceInfo = permissionRequest.get().dataSourceInformation();
        }

        var rawString = response.apiResponse().toString();

        return new RawDataMessage(response.permissionId(), response.connectionId(), response.dataNeedId(),
                dataSourceInfo, ZonedDateTime.now(ZoneId.of("UTC")), rawString);
    }

    @Override
    public void close() {
        // complete is emitted when identifiableApiResponseFlux completes
    }
}
