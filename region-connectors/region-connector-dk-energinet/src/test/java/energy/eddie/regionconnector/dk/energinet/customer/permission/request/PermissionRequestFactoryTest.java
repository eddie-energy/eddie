package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        var start = ZonedDateTime.now().minusDays(10);
        var end = start.plusDays(5);
        var requestForCreation = new PermissionRequestForCreation("foo", start, end, "token",
                PeriodResolutionEnum.PT1H, "bar", "poo");

        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        EnerginetConfiguration conf = mock(EnerginetConfiguration.class);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(permissionRequestRepository, permissionStateMessages, conf);

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(requestForCreation);

        // Then
        assertNotNull(permissionRequest);
    }
}
