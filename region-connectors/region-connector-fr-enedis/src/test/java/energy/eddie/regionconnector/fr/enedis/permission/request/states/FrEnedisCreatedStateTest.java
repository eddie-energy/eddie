package energy.eddie.regionconnector.fr.enedis.permission.request.states;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrEnedisCreatedStateTest {
    @Test
    void validate_changesToValidatedState_whenValid() throws ValidationException {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
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
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = start.minusDays(1);
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", start, end, Granularity.P1D, factory);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, factory);

        // When, Then
        assertThrows(ValidationException.class, createdState::validate);
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndIsFurtherThanThreeYearsInTheFuture() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = start.plusYears(4);
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest("cid", "dnid", start, end, Granularity.P1D, factory);
        FrEnedisCreatedState createdState = new FrEnedisCreatedState(permissionRequest, factory);

        // When, Then
        assertThrows(ValidationException.class, createdState::validate);
        assertEquals(FrEnedisMalformedState.class, permissionRequest.state().getClass());
    }
}
