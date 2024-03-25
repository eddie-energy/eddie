package energy.eddie.regionconnector.shared.permission.requests.validation;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartIsBeforeOrEqualEndValidatorTest {
    @Test
    void testValidateWhen_startIsBeforeEnd() {
        // Given
        LocalDate startTime = LocalDate.now(ZoneOffset.UTC);
        LocalDate endTime = startTime.plusDays(1);
        PermissionRequest request = new SimpleTimeFramePermissionRequest(startTime, endTime);
        StartIsBeforeOrEqualEndValidator<PermissionRequest> validator = new StartIsBeforeOrEqualEndValidator<>();

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWhen_startIsEqualToEnd() {
        // Given
        LocalDate startTime = LocalDate.now(ZoneOffset.UTC);
        PermissionRequest request = new SimpleTimeFramePermissionRequest(startTime, startTime);
        StartIsBeforeOrEqualEndValidator<PermissionRequest> validator = new StartIsBeforeOrEqualEndValidator<>();

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateWhen_startIsAfterEnd() {
        // Given
        LocalDate startTime = LocalDate.now(ZoneOffset.UTC);
        LocalDate endTime = startTime.minusDays(1);
        PermissionRequest request = new SimpleTimeFramePermissionRequest(startTime, endTime);
        StartIsBeforeOrEqualEndValidator<PermissionRequest> validator = new StartIsBeforeOrEqualEndValidator<>();

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertFalse(errors.isEmpty());
    }
}
