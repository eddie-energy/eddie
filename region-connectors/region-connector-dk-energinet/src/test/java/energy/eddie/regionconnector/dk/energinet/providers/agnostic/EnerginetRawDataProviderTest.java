package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnerginetRawDataProviderTest {
    @Mock
    private DkPermissionRequestRepository mockRepo;

    @Test
    void givenValueOnFlux_publishesOnFlow() {
        // Given
        TestPublisher<IdentifiableApiResponse> publisher = TestPublisher.create();
        var permissionRequest = new EnerginetPermissionRequest(
                "foo",
                "bar",
                "dId",
                "meteringPointId",
                "refreshToken",
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.PT1H,
                "accessToken",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        var reading = new IdentifiableApiResponse(permissionRequest, new MyEnergyDataMarketDocumentResponse());
        when(mockRepo.findByPermissionId(any()))
                .thenReturn(Optional.of(permissionRequest));

        //noinspection resource StepVerifier closes provider
        var provider = new EnerginetRawDataProvider(publisher.flux(), mockRepo, new ObjectMapper());

        StepVerifier.create(provider.getRawDataStream().log())
                    // When
                    .then(() -> publisher.next(reading))
                    // Then
                    .expectNextCount(1)
                    .thenCancel()
                    .verify(Duration.ofSeconds(2));
    }

    @Test
    void givenValueOnFlux_publishesOnFlowWithUnknownPermissionRequest() {
        // Given
        TestPublisher<IdentifiableApiResponse> publisher = TestPublisher.create();
        var permissionRequest = new EnerginetPermissionRequest(
                "foo",
                "bar",
                "dId",
                "meteringPointId",
                "refreshToken",
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.PT1H,
                "accessToken",
                PermissionProcessStatus.ACCEPTED,
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        var reading = new IdentifiableApiResponse(permissionRequest, new MyEnergyDataMarketDocumentResponse());

        //noinspection resource StepVerifier closes provider
        var provider = new EnerginetRawDataProvider(publisher.flux(), mockRepo, new ObjectMapper());

        StepVerifier.create(provider.getRawDataStream().log())
                    // When
                    .then(() -> publisher.next(reading))
                    // Then
                    .expectNextCount(1)
                    .thenCancel()
                    .verify(Duration.ofSeconds(2));
    }
}
