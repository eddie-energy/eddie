package energy.eddie.regionconnector.dk.energinet.providers.agnostic;

import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.persistence.DkEnerginetCustomerPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class EnerginetRawDataProviderTest {
    @Mock
    private DkEnerginetCustomerPermissionRequestRepository mockRepo;

    @Test
    void givenValueOnFlux_publishesOnFlow() {
        // Given
        TestPublisher<IdentifiableApiResponse> publisher = TestPublisher.create();
        var reading = new IdentifiableApiResponse("foo", "bar", "dId", mock(MyEnergyDataMarketDocumentResponse.class));

        //noinspection resource StepVerifier closes provider
        var provider = new EnerginetRawDataProvider(publisher.flux(), mockRepo);

        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getRawDataStream()).log())
                // When
                .then(() -> publisher.next(reading))
                // Then
                .expectNextCount(1)
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }
}