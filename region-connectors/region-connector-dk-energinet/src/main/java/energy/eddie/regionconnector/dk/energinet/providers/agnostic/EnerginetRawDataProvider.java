package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final ObjectMapper objectMapper;

    public EnerginetRawDataProvider(
            Flux<IdentifiableApiResponse> identifiableApiResponseFlux,
            DkPermissionRequestRepository repository,
            // Spring uses the autoconfigured object mapper for some reason without a qualifier.
            @Qualifier("objectMapper") ObjectMapper objectMapper
    ) {
        this.identifiableApiResponseFlux = identifiableApiResponseFlux;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return identifiableApiResponseFlux
                .mapNotNull(this::createRawDataMessage);
    }

    @Nullable
    private RawDataMessage createRawDataMessage(IdentifiableApiResponse response) {
        var pr = response.permissionRequest();
        String permissionId = pr.permissionId();
        var permissionRequest = repository.findByPermissionId(permissionId);
        DataSourceInformation dataSourceInfo = null;

        if (permissionRequest.isEmpty()) {
            LOGGER.error("No permission with ID {} found in repository.", permissionId);
        } else {
            dataSourceInfo = permissionRequest.get().dataSourceInformation();
        }
        try {
            String rawString = objectMapper.writeValueAsString(response.apiResponse());
            return new RawDataMessage(permissionId,
                                      pr.connectionId(),
                                      pr.dataNeedId(),
                                      dataSourceInfo,
                                      ZonedDateTime.now(ZoneId.of("UTC")),
                                      rawString);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error serializing raw data message.", e);
            return null;
        }
    }

    @Override
    public void close() {
        // complete is emitted when identifiableApiResponseFlux completes
    }
}
