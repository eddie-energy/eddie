package energy.eddie.regionconnector.dk.energinet.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Set;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        var start = ZonedDateTime.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var requestForCreation = new PermissionRequestForCreation("foo", start, end, "token",
                Granularity.PT1H, "bar", "foo");
        EnerginetCustomerApi customerApi = mock(EnerginetCustomerApi.class);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(customerApi, Set.of(), new StateBuilderFactory());

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(requestForCreation);

        // Then
        assertNotNull(permissionRequest);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRecreatePermissionRequest_doesNotRunExtension() {
        var start = ZonedDateTime.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(5);
        var requestForCreation = new PermissionRequestForCreation("foo", start, end, "token",
                Granularity.PT1H, "bar", "foo");
        EnerginetCustomerApi customerApi = mock(EnerginetCustomerApi.class);
        var permissionRequest = new EnerginetCustomerPermissionRequest("pid", requestForCreation, customerApi, new StateBuilderFactory());
        var extension = mock(Extension.class);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(customerApi, Set.of(extension), new StateBuilderFactory());

        // When
        PermissionRequest wrapped = permissionRequestFactory.create(permissionRequest);

        // Then
        assertNotNull(wrapped);
        verify(extension, never()).accept(any());
    }
}