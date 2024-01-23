package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(Set.of());
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation("cid", "dnid", start, end);

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(permissionRequestForCreation);

        // Then
        assertNotNull(permissionRequest);
    }

    @Test
    void testCreatePermissionRequest_withExistingPermissionRequest() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusDays(1);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(Set.of());
        TimeframedPermissionRequest original = new EnedisPermissionRequest("cid", "dnid", start, end);

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(original);

        // Then
        assertNotNull(permissionRequest);
    }
}