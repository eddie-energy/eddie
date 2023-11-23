package energy.eddie.regionconnector.at.eda.permission.request.validators;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.permission.request.validation.StartIsBeforeOrEqualEndValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StartIsBeforeOrEqualEndValidatorTest {
    @Test
    void isValid_whenPermissionRequestEndNull() {
        // Given
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                Optional.empty(),
                LocalDate.now(ZoneId.systemDefault()),
                Optional.empty(),
                new AtAcceptedPermissionRequestState(null)
        );

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void isNotValid_whenPermissionRequestEndIsBeforeStart() {
        // Given
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = start.minusDays(1);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                Optional.empty(),
                start,
                Optional.of(end),
                new AtAcceptedPermissionRequestState(null)
        );
        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("dataFrom", res.get(0).name())
        );
    }

    @Test
    void isValid_whenPermissionRequestStartIsBeforeEnd() {
        // Given
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        LocalDate end = start.plusDays(1);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                Optional.empty(),
                start,
                Optional.of(end),
                new AtAcceptedPermissionRequestState(null)
        );

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void isValid_whenPermissionRequestStartIsEqualEnd() {
        // Given
        StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
        LocalDate start = LocalDate.now(ZoneId.systemDefault());
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                Optional.empty(),
                start,
                Optional.empty(),
                new AtAcceptedPermissionRequestState(null)
        );
        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }
}