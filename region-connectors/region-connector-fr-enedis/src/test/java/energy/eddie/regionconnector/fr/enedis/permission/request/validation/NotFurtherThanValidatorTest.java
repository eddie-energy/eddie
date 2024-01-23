package energy.eddie.regionconnector.fr.enedis.permission.request.validation;

import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotFurtherThanValidatorTest {
    @Test
    void test_validate_when_endDateIsWithinLimit() {
        // Given
        TimeframedPermissionRequest request = new EnedisPermissionRequest("cid", "dnid", ZonedDateTime.now(ZoneOffset.UTC), ZonedDateTime.now(ZoneOffset.UTC).plusHours(1));
        NotFurtherThanValidator validator = new NotFurtherThanValidator(ChronoUnit.DAYS, 1);

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void test_validate_when_endDateIsOutOfBounds() {
        // Given
        TimeframedPermissionRequest request = new EnedisPermissionRequest("cid", "dnid", ZonedDateTime.now(ZoneOffset.UTC), ZonedDateTime.now(ZoneOffset.UTC).plusDays(2));
        NotFurtherThanValidator validator = new NotFurtherThanValidator(ChronoUnit.DAYS, 1);

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertFalse(errors.isEmpty());
    }
}