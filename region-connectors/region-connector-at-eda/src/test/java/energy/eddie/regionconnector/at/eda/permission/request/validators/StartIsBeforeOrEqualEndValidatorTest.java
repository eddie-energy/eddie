package energy.eddie.regionconnector.at.eda.permission.request.validators;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.permission.request.validation.StartIsBeforeOrEqualEndValidator;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
                "dsoId",
                Optional.empty(),
                ZonedDateTime.now(ZoneOffset.UTC),
                null,
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
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.minusDays(1);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                start,
                end,
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
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime end = start.plusDays(1);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                start,
                end,
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
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                start,
                start,
                new AtAcceptedPermissionRequestState(null)
        );
        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }
}