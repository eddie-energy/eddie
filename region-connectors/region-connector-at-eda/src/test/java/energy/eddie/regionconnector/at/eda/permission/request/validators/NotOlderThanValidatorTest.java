package energy.eddie.regionconnector.at.eda.permission.request.validators;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.permission.request.validation.NotOlderThanValidator;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NotOlderThanValidatorTest {

    @Test
    void notValid_whenDateIsTooOld() {
        // Given
        NotOlderThanValidator validator = new NotOlderThanValidator(ChronoUnit.DAYS, 10);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                ZonedDateTime.now(ZoneOffset.UTC).minusDays(15),
                null,
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
    void valid_whenDateIsNotTooOld() {
        // Given
        NotOlderThanValidator validator = new NotOlderThanValidator(ChronoUnit.DAYS, 10);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                ZonedDateTime.now(ZoneOffset.UTC).minusDays(5),
                null,
                new AtAcceptedPermissionRequestState(null)
        );

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void valid_whenFromIsInTheFuture() {
        // Given
        NotOlderThanValidator validator = new NotOlderThanValidator(ChronoUnit.DAYS, 10);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(5),
                null,
                new AtAcceptedPermissionRequestState(null)
        );

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void valid_whenDateIsOnTheSameDay() {
        // Given
        NotOlderThanValidator validator = new NotOlderThanValidator(ChronoUnit.DAYS, 0);
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
}