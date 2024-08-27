package energy.eddie.regionconnector.es.datadis.providers.agnostic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.RawDataMessage;
import energy.eddie.api.agnostic.RawDataProvider;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@ConditionalOnProperty(name = "eddie.raw.data.output.enabled", havingValue = "true")
public class DatadisRawDataProvider implements RawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisRawDataProvider.class);
    private final Flux<IdentifiableMeteringData> meteringDataFlux;
    private final ObjectMapper objectMapper;

    public DatadisRawDataProvider(
            Flux<IdentifiableMeteringData> meteringDataFlux,
            ObjectMapper objectMapper
    ) {
        this.meteringDataFlux = meteringDataFlux;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<RawDataMessage> getRawDataStream() {
        return meteringDataFlux
                .mapNotNull(this::createRawDataMessage);
    }

    @Nullable
    private RawDataMessage createRawDataMessage(IdentifiableMeteringData meteringData) {
        PermissionRequest request = meteringData.permissionRequest();
        try {
            String rawString = objectMapper.writeValueAsString(meteringData.intermediateMeteringData().meteringData());
            return new RawDataMessage(
                    request.permissionId(),
                    request.connectionId(),
                    request.dataNeedId(),
                    request.dataSourceInformation(),
                    ZonedDateTime.now(ZoneId.of("UTC")),
                    rawString
            );
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error serializing metering data", e);
            return null;
        }
    }

    @Override
    public void close() {
        // complete is emitted when meteringDataFlux completes
    }
}
