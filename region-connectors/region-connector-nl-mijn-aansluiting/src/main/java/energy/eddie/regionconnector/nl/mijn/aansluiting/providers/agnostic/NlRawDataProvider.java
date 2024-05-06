package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.agnostic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class NlRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(NlRawDataProvider.class);
    private final Flux<RawDataMessage> flux;
    private final ObjectMapper objectMapper;

    public NlRawDataProvider(PollingService pollingService, ObjectMapper objectMapper) {
        this.flux = pollingService.identifiableMeteredDataFlux()
                                  .mapNotNull(this::toRawData);
        this.objectMapper = objectMapper;
    }

    @Nullable
    private RawDataMessage toRawData(IdentifiableMeteredData identifiableMeteredData) {
        var pr = identifiableMeteredData.permissionRequest();
        var permissionId = pr.permissionId();
        try {
            return new RawDataMessage(
                    permissionId,
                    pr.connectionId(),
                    pr.dataNeedId(),
                    pr.dataSourceInformation(),
                    ZonedDateTime.now(ZoneOffset.UTC),
                    objectMapper.writeValueAsString(identifiableMeteredData.meteredData())
            );
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error while trying to serialize raw data for permission request {}", permissionId, e);
            return null;
        }
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return flux;
    }

    @Override
    public void close() {
        // No-Op
    }
}
