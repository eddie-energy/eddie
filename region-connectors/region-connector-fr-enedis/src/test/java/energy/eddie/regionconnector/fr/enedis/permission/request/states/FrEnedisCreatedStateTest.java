package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrEnedisCreatedStateTest {
    @Test
    void validate_changesToValidatedState_whenValid() throws ValidationException {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", now, now.plusDays(1), Granularity.P1D, factory);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, factory);

        // When
        createdState.validate();

        // Then
        assertEquals(FrEnedisValidatedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndBeforeStart() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.minusDays(1);
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", start, end, Granularity.P1D, factory);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest,factory);

        // When, Then
        assertThrows(ValidationException.class, createdState::validate);
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndIsFurtherThanThreeYearsInTheFuture() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusYears(4);
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", start, end, Granularity.P1D, factory);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, factory);

        // When, Then
        assertThrows(ValidationException.class, createdState::validate);
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
    }
}