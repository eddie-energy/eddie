package energy.eddie.aiida.constraints;

import energy.eddie.aiida.dtos.PermissionDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpirationTimeNotInPastPermissionDtoValidatorTest {
    private Validator validator;

    @BeforeEach
    public void setUp() {
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            validator = validatorFactory.getValidator();
        }
    }

    @Test
    void givenNull_validation_willFail() {
        var start = Instant.now().minusSeconds(100);
        var dto = new PermissionDto(UUID.randomUUID().toString(), "SomeName", "dId",
                                    start, null, start, "conId", Set.of("1-0:1.8.0"));

        var violations = validator.validate(dto);
        assertEquals(3, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("expirationTime must not be null.",
                "must not be null.", // error once because of @NotNull and once because of @ExpirationTimeNotInPast
                "expirationTime must not be null.",
                "startTime and expirationTime must not be null."));
    }

    @Test
    void givenExpirationTimeInPast_validation_willFail() {
        var start = Instant.now().minusSeconds(100);
        var expiration = Instant.now().minusSeconds(50);
        var dto = new PermissionDto(UUID.randomUUID().toString(), "SomeName", "dId",
                                    start, expiration, start, "conId", Set.of("1-0:1.8.0"));

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("expirationTime must not lie in the past."));
    }
}
