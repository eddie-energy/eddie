package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrEnedisCreatedStateTest {

    @Test
    void validate_changesToValidatedState_whenValid() throws ValidationException {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", now, now.plusDays(1));
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest);

        // When
        createdState.validate();

        // Then
        assertEquals(FrEnedisValidatedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndBeforeStart() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.minusDays(1);
        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", start, end);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest);

        // When, Then
        assertThrows(ValidationException.class, createdState::validate);
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndIsFurtherThanThreeYearsInTheFuture() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = start.plusYears(4);
        TimeframedPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", start, end);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest);

        // When, Then
        assertThrows(ValidationException.class, createdState::validate);
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
    }
}