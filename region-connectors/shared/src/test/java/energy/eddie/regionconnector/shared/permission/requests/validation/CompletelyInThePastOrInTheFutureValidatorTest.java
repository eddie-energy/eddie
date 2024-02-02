package energy.eddie.regionconnector.shared.permission.requests.validation;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletelyInThePastOrInTheFutureValidatorTest {
    @Test
    void notValid_whenFromAndToAreNotCompletelyInThePast() {
        // Given
        var validator = new CompletelyInThePastOrInTheFutureValidator<>();
        TimeframedPermissionRequest permissionRequest = new SimpleTimeFramePermissionRequest(
                ZonedDateTime.now(ZoneOffset.UTC).minusDays(5),
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(5)
        );

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("start", res.getFirst().name())
        );
    }

    @Test
    void isValid_whenStartAndEndDateInThePast() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = now.minusDays(11);
        ZonedDateTime end = now.minusDays(1);
        var validator = new CompletelyInThePastOrInTheFutureValidator<>();
        TimeframedPermissionRequest permissionRequest = new SimpleTimeFramePermissionRequest(start, end);

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
        TimeframedPermissionRequest permissionRequest = new SimpleTimeFramePermissionRequest(start, end);
        var validator = new CompletelyInThePastOrInTheFutureValidator<>();

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void isValid_whenStartDateIsNow() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureValidator<>();
        TimeframedPermissionRequest permissionRequest = new SimpleTimeFramePermissionRequest(now, null);


        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void isValid_whenStartDateIsInTheFutureAndDataToEmpty() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureValidator<>();
        TimeframedPermissionRequest permissionRequest = new SimpleTimeFramePermissionRequest(now.plusDays(10), null);

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertEquals(0, res.size());
    }

    @Test
    void isNotValid_whenStartDateIsInThePastAndDataToEmpty() {
        // Given
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        var validator = new CompletelyInThePastOrInTheFutureValidator<>();
        TimeframedPermissionRequest permissionRequest = new SimpleTimeFramePermissionRequest(now.minusDays(10), null);

        // When
        var res = validator.validate(permissionRequest);

        // Then
        assertAll(
                () -> assertEquals(1, res.size()),
                () -> assertEquals("start", res.getFirst().name())
        );
    }
}