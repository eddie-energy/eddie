package energy.eddie.regionconnector.shared.permission.requests.validation;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletelyInThePastOrInTheFutureValidatorTest {
    @Test
    void notValid_whenFromAndToAreNotCompletelyInThePast() {
        // Given
        var validator = new CompletelyInThePastOrInTheFutureValidator<>();
        TimeframedPermissionRequest permissionRequest = new SimpleTimeFramePermissionRequest(
                LocalDate.now(ZoneOffset.UTC).minusDays(5),
                LocalDate.now(ZoneOffset.UTC).plusDays(5)
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
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = now.minusDays(11);
        LocalDate end = now.minusDays(1);
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
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = now.plusDays(1);
        LocalDate end = now.plusDays(1);
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
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
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
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
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
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
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
