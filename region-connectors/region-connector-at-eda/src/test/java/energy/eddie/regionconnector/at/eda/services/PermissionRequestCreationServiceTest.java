package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionRequestCreationServiceTest {

    @Test
    void createValidPermissionRequest() throws StateTransitionException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        AtConfiguration config = mock(AtConfiguration.class);
        when(config.eligiblePartyId()).thenReturn("AT999999");

        LocalDate start = LocalDate.now(Clock.systemUTC()).minusDays(10);
        LocalDate end = start.plusDays(5);
        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100", "dnid", "AT000000", start, end, Granularity.PT15M);
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(edaAdapter, Set.of());
        PermissionRequestCreationService creationService = new PermissionRequestCreationService(permissionRequestFactory, config);

        // When
        var res = creationService.createAndSendPermissionRequest(pr);

        // Then
        assertNotNull(res);
    }
}