package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        var start = ZonedDateTime.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var requestForCreation = new PermissionRequestForCreation("foo", start, end, "token",
                Granularity.PT1H, "bar", "poo");

        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        EnerginetCustomerApi customerApi = mock(EnerginetCustomerApi.class);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(permissionRequestRepository, permissionStateMessages, customerApi);

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(requestForCreation);

        // Then
        assertNotNull(permissionRequest);

        // Clean-Up
        permissionRequestFactory.close();
    }

    @Test
    void close_emitsCompleteOnPublisher() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        EnerginetCustomerApi customerApi = mock(EnerginetCustomerApi.class);
        PermissionRequestFactory factory = new PermissionRequestFactory(permissionRequestRepository, permissionStateMessages, customerApi);

        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(factory.getConnectionStatusMessageStream()))
                .expectComplete()
                .verifyLater();

        // When
        factory.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }
}