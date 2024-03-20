package energy.eddie.regionconnector.fr.enedis.permission.request.validation;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotFurtherThanValidatorTest {
    @Test
    void test_validate_when_endDateIsWithinLimit() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        TimeframedPermissionRequest request = new EnedisPermissionRequest("cid",
                                                                          "dnid",
                                                                          LocalDate.now(ZoneOffset.UTC),
                                                                          LocalDate.now(ZoneOffset.UTC),
                                                                          Granularity.P1D,
                                                                          factory);
        NotFurtherThanValidator validator = new NotFurtherThanValidator(ChronoUnit.DAYS, 1);

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void test_validate_when_endDateIsOutOfBounds() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory();
        TimeframedPermissionRequest request = new EnedisPermissionRequest("cid",
                                                                          "dnid",
                                                                          LocalDate.now(ZoneOffset.UTC),
                                                                          LocalDate.now(ZoneOffset.UTC).plusDays(2),
                                                                          Granularity.P1D,
                                                                          factory);
        NotFurtherThanValidator validator = new NotFurtherThanValidator(ChronoUnit.DAYS, 1);

        // When
        List<AttributeError> errors = validator.validate(request);

        // Then
        assertFalse(errors.isEmpty());
    }
}
