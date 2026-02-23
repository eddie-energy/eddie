package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeRawDataProviderTest {

    @Mock
    private ValidatedHistoricalDataStream stream;

    private DeRawDataProvider provider;

    @BeforeEach
    void setUp() {
        DePermissionRequest permissionRequest = new DePermissionRequestBuilder()
                .permissionId("perm-123")
                .connectionId("conn-1")
                .dataNeedId("need-1")
                .build();

        EtaPlusMeteredData meteredData = new EtaPlusMeteredData(
                "malo-1",
                LocalDate.of(2024, 10, 1),
                LocalDate.of(2024, 12, 31),
                List.of(),
                "{\"raw\":\"data\"}"
        );

        IdentifiableValidatedHistoricalData identifiableData =
                new IdentifiableValidatedHistoricalData(permissionRequest, meteredData);

        when(stream.validatedHistoricalData()).thenReturn(Flux.just(identifiableData));

        provider = new DeRawDataProvider(stream);
    }

    @Test
    @DisplayName("Should convert stream data to RawDataMessages with correct permissionId and raw payload")
    void getRawDataStreamShouldConvertToRawDataMessages() {
        StepVerifier.create(provider.getRawDataStream())
                .assertNext(message -> {
                    assertThat(message.permissionId()).isEqualTo("perm-123");
                    assertThat(message.rawPayload()).isEqualTo("{\"raw\":\"data\"}");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should complete without throwing when close is called")
    void closeShouldCompleteWithoutException() {
        assertThatCode(() -> provider.close()).doesNotThrowAnyException();
    }
}
