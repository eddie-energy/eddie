package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.agnostic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.MijnAansluitingResponse;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.request.MijnAansluitingPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.JsonResourceObjectMapper;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NlRawDataProviderTest {
    private final JsonResourceObjectMapper<List<MijnAansluitingResponse>> jsonResourceObjectMapper =
            new JsonResourceObjectMapper<>(new TypeReference<>() {});
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new Jdk8Module());
    @Mock
    private PollingService pollingService;

    @Test
    void testMapping_emitsCorrectJson() throws IOException {
        // Given
        var response = jsonResourceObjectMapper.loadTestJson("consumption_data.json");
        var pr = new MijnAansluitingPermissionRequest(
                "pid",
                "cid",
                "dnid",
                PermissionProcessStatus.ACCEPTED,
                null,
                null,
                ZonedDateTime.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.P1D
        );
        var doc = new IdentifiableMeteredData(pr, response);
        when(pollingService.identifiableMeteredDataFlux()).thenReturn(Flux.just(doc));

        // When
        var rawDataProvider = new NlRawDataProvider(pollingService, objectMapper);

        // Then
        StepVerifier.create(rawDataProvider.getRawDataStream())
                    .assertNext(rawData -> assertAll(
                            () -> assertEquals("pid", rawData.permissionId()),
                            () -> assertEquals("cid", rawData.connectionId()),
                            () -> assertEquals("dnid", rawData.dataNeedId()),
                            () -> assertNotNull(rawData.rawPayload())
                    ))
                    .verifyComplete();
    }
}