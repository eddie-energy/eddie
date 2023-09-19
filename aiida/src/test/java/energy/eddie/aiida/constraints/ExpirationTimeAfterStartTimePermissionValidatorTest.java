package energy.eddie.aiida.constraints;

import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
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

class ExpirationTimeAfterStartTimePermissionValidatorTest {
    private ValidatorFactory validatorFactory;
    private Validator validator;
    private Instant start;
    private Instant end;
    private Instant grant;
    private String name;
    private String connectionId;
    private Set<String> codes;
    private KafkaStreamingConfig streamingConfig;

    @BeforeEach
    public void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        // valid parameters
        name = "My Test Service";
        connectionId = "RandomId";
        start = Instant.now();
        end = start.plusSeconds(5000);

        String bootstrapServers = "localhost:9092";
        String validDataTopic = "ValidPublishTopic";
        String validStatusTopic = "ValidStatusTopic";
        String validSubscribeTopic = "ValidSubscribeTopic";
        streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

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
        var dto = new Permission(name, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(dto);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("expirationTime has to be after startTime.", first.getMessage());
    }

    @Test
    void givenNull_validation_willFail() {
        var dto = new Permission(name, null, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(dto);
        assertEquals(2, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("startTime and expirationTime mustn't be null.",
                "startTime mustn't be null."));


        dto = new Permission(name, start, null, grant, connectionId, codes, streamingConfig);

        violations = validator.validate(dto);
        assertEquals(2, violations.size());
        list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("startTime and expirationTime mustn't be null.",
                "expirationTime mustn't be null."));
    }

    @Test
    void givenValidInput_validation_passes() {
        var dto = new Permission(name, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(dto);
        assertEquals(0, violations.size());
    }
}