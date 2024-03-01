package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PermissionRequestCreationAndValidationServiceTest {

    @Test
    void createValidPermissionRequest() throws ValidationException {
        // Given
        AtConfiguration config = mock(AtConfiguration.class);
        when(config.eligiblePartyId()).thenReturn("AT999999");


        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        ZonedDateTime end = start.plusDays(5);
        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT0000000699900000000000206868100",
                                                                           "dnid", "AT000000", start, end,
                                                                           Granularity.PT15M);
        PermissionRequestCreationAndValidationService creationService = new PermissionRequestCreationAndValidationService(
                config,
                mock(Outbox.class)
        );

        // When
        var res = creationService.createAndValidatePermissionRequest(pr);

        // Then
        assertNotNull(res);
    }

    @Test
    void createInvalidPermissionRequest() {
        // Given
        AtConfiguration config = mock(AtConfiguration.class);
        when(config.eligiblePartyId()).thenReturn("AT999999");


        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10);
        ZonedDateTime end = start.plusDays(5);
        PermissionRequestForCreation pr = new PermissionRequestForCreation("cid", "AT1234500699900000000000206868100",
                                                                           "dnid", "AT000000", start, end,
                                                                           Granularity.PT15M);
        Outbox outbox = mock(Outbox.class);
        PermissionRequestCreationAndValidationService creationService = new PermissionRequestCreationAndValidationService(
                config,
                outbox
        );

        // When
        // Then
        assertThrows(ValidationException.class, () -> creationService.createAndValidatePermissionRequest(pr));
        verify(outbox, times(2)).commit(any());
    }
}