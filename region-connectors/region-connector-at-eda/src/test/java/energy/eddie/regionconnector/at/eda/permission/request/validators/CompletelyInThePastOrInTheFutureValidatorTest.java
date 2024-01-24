package energy.eddie.regionconnector.at.eda.permission.request.validators;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.permission.request.validation.CompletelyInThePastOrInTheFutureValidator;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletelyInThePastOrInTheFutureValidatorTest {
    @Test
    void notValid_whenFromAndToAreNotCompletelyInThePast() {
        // Given
        CompletelyInThePastOrInTheFutureValidator validator = new CompletelyInThePastOrInTheFutureValidator();
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                ZonedDateTime.now(ZoneOffset.UTC).minusDays(5),
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(5),
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
    void isValid_whenStartAndEndDateInThePast() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = now.minusDays(11);
        ZonedDateTime end = now.minusDays(1);
        CompletelyInThePastOrInTheFutureValidator validator = new CompletelyInThePastOrInTheFutureValidator();
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
    void isValid_whenStartAndEndDateInTheFuture() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = now.plusDays(1);
        ZonedDateTime end = now.plusDays(1);
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
        CompletelyInThePastOrInTheFutureValidator validator = new CompletelyInThePastOrInTheFutureValidator();

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void isValid_whenStartDateIsNow() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        CompletelyInThePastOrInTheFutureValidator validator = new CompletelyInThePastOrInTheFutureValidator();
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                now,
                null,
                new AtAcceptedPermissionRequestState(null)
        );

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void isValid_whenStartDateIsInTheFutureAndDataToEmpty() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        CompletelyInThePastOrInTheFutureValidator validator = new CompletelyInThePastOrInTheFutureValidator();
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                now.plusDays(10),
                null,
                new AtAcceptedPermissionRequestState(null)
        );

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void isNotValid_whenStartDateIsInThePastAndDataToEmpty() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        CompletelyInThePastOrInTheFutureValidator validator = new CompletelyInThePastOrInTheFutureValidator();
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                now.minusDays(10),
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
}