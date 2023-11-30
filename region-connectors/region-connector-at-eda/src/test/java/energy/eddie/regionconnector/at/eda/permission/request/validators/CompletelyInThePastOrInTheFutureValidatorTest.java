package energy.eddie.regionconnector.at.eda.permission.request.validators;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.permission.request.validation.CompletelyInThePastOrInTheFutureValidator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
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
                LocalDate.now(ZoneId.systemDefault()).minusDays(5),
                Optional.of(LocalDate.now(ZoneId.systemDefault()).plusDays(5)),
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
        LocalDate now = LocalDate.now(Clock.systemUTC());
        LocalDate start = now.minusDays(11);
        LocalDate end = now.minusDays(1);
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
                Optional.of(end),
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
        LocalDate now = LocalDate.now(Clock.systemUTC());
        LocalDate start = now.plusDays(1);
        LocalDate end = now.plusDays(11);
        AtPermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                "cmId",
                "convId",
                "dsoId",
                Optional.empty(),
                start,
                Optional.of(end),
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
        LocalDate now = LocalDate.now(Clock.systemUTC());
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
                Optional.empty(),
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
        LocalDate now = LocalDate.now(Clock.systemUTC());
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
                Optional.empty(),
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
        LocalDate now = LocalDate.now(Clock.systemUTC());
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
                Optional.empty(),
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