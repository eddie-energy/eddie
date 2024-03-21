package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = start.plusDays(1);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(Set.of(),
                                                                                         new StateBuilderFactory());
        PermissionRequestForCreation permissionRequestForCreation = new PermissionRequestForCreation("cid", "dnid");

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(permissionRequestForCreation,
                                                                              start,
                                                                              end,
                                                                              Granularity.P1D);

        // Then
        assertNotNull(permissionRequest);
    }

    @Test
    void testCreatePermissionRequest_withExistingPermissionRequest() {
        // Given
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = start.plusDays(1);
        StateBuilderFactory factory = new StateBuilderFactory();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(Set.of(), factory);
        FrEnedisPermissionRequest original = new EnedisPermissionRequest("cid",
                                                                         "dnid",
                                                                         start,
                                                                         end,
                                                                         Granularity.P1D,
                                                                         factory);

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(original);

        // Then
        assertNotNull(permissionRequest);
    }
}
