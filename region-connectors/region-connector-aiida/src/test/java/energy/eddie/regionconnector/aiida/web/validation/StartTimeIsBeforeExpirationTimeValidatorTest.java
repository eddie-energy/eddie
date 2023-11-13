package energy.eddie.regionconnector.aiida.web.validation;

import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import jakarta.validation.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartTimeIsBeforeExpirationTimeValidatorTest {
    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    public void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    public void tearDown() {
        validatorFactory.close();
    }

    @Test
    void givenNull_validation_fails() {
        var dto = new PermissionRequestForCreation("TestConnectionId", "TestDataNeedId", null, Instant.now());

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("Permission request must have a startTime and expirationTime"));

        dto = new PermissionRequestForCreation("TestConnectionId", "TestDataNeedId", Instant.now(), null);

        violations = validator.validate(dto);
        assertEquals(1, violations.size());
        list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("Permission request must have a startTime and expirationTime"));
    }

    @Test
    void givenStartTimeAfterExpirationTime_validation_fails() {
        var expiration = Instant.now();
        var start = expiration.plusSeconds(1000);
        var dto = new PermissionRequestForCreation("TestConnectionId", "TestDataNeedId", start, expiration);

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("startTime must be before expirationTime"));
    }

    @Test
    void givenSameStartTimeAndExpirationTime_validation_fails() {
        var now = Instant.now();
        var dto = new PermissionRequestForCreation("TestConnectionId", "TestDataNeedId", now, now);

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("startTime must be before expirationTime"));
    }

    @Test
    void givenValidInput_validation_passes() {
        var start = Instant.now();
        var expiration = start.plusSeconds(1000);
        var dto = new PermissionRequestForCreation("TestConnectionId", "TestDataNeedId", start, expiration);

        var violations = validator.validate(dto);
        assertEquals(0, violations.size());
    }


    @Test
    void givenRequestIsNull_validation_fails() {
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        when(context.buildConstraintViolationWithTemplate(any()))
                .thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.class));
        var validator = new StartTimeIsBeforeExpirationTimeValidator();

        boolean result = validator.isValid(null, context);

        assertFalse(result);
    }
}