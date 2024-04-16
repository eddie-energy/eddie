package energy.eddie.aiida.constraints;

import energy.eddie.aiida.dtos.PermissionDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpirationTimeAfterStartTimePermissionDtoValidatorTest {
    private ValidatorFactory validatorFactory;
    private Validator validator;
    private Instant start;
    private Instant end;
    private Instant grant;
    private String permissionId;
    private String name;
    private String connectionId;
    private String dataNeedId;
    private Set<String> codes;

    @BeforeEach
    public void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        // valid parameters
        permissionId = "08770fd7-84b5-4b4e-9db5-9b6066bb2af5";
        name = "My Test Service";
        connectionId = "RandomId";
        dataNeedId = "dataNeedId";
        start = Instant.now();
        end = start.plusSeconds(5000);

        codes = Set.of("1.8.0", "2.8.0");
        grant = Instant.now();
    }

    @AfterEach
    public void tearDown() {
        validatorFactory.close();
    }

    @Test
    void givenExpirationTimeBeforeStartTime_validation_willFail() {
        end = start.minusSeconds(1000);
        var dto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, connectionId, codes);

        var violations = validator.validate(dto);
        assertEquals(2, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("expirationTime has to be after startTime.",
                "expirationTime must not lie in the past."));
    }

    @Test
    void givenNull_validation_willFail() {
        var dto = new PermissionDto(permissionId, name, dataNeedId, null, end, grant, connectionId, codes);

        var violations = validator.validate(dto);
        assertEquals(2, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("startTime and expirationTime must not be null.",
                "must not be null."));


        dto = new PermissionDto(permissionId, name, dataNeedId, start, null, grant, connectionId, codes);

        violations = validator.validate(dto);
        assertEquals(3, violations.size());
        list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("startTime and expirationTime must not be null.",
                "expirationTime must not be null.",
                "must not be null."));
    }

    @Test
    void givenValidInput_validation_passes() {
        var dto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, connectionId, codes);

        var violations = validator.validate(dto);
        assertEquals(0, violations.size());
    }
}
