package energy.eddie.aiida.dtos;

import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionDtoTest {
    private Validator validator;
    private Instant start;
    private Instant end;
    private Instant grant;
    private String name;
    private String connectionId;
    private Set<String> codes;
    private String bootstrapServers;
    private String validDataTopic;
    private String validStatusTopic;
    private String validSubscribeTopic;
    private KafkaStreamingConfig streamingConfig;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        // valid parameters
        name = "My Test Service";
        connectionId = "RandomId";
        start = Instant.now();
        end = start.plusSeconds(5000);

        bootstrapServers = "localhost:9092";
        validDataTopic = "ValidPublishTopic";
        validStatusTopic = "ValidStatusTopic";
        validSubscribeTopic = "ValidSubscribeTopic";
        streamingConfig = new KafkaStreamingConfig(bootstrapServers, validDataTopic, validStatusTopic, validSubscribeTopic);

        codes = Set.of("1.8.0", "2.8.0");
        grant = Instant.now();
    }

    @AfterEach
    public void tearDown() {
        validatorFactory.close();
    }

    @Test
    void givenExpirationTimeBeforeStartTime_validation_fails() {
        end = start.minusSeconds(1000);

        var permissionDto = new PermissionDto(name, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permissionDto);
        System.out.println(violations);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("expirationTime has to be after startTime.", first.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void givenEmptyConnectionIdOrServiceName_validation_fails(String str) {
        var permissionDto = new PermissionDto(str, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());

        permissionDto = new PermissionDto(name, start, end, grant, str, codes, streamingConfig);

        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
    }

    @Test
    void givenNull_validation_fails() {
        PermissionDto permissionDto = new PermissionDto(null, start, end, grant, connectionId, codes, streamingConfig);
        var violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("serviceName mustn't be null or blank.", first.getMessage());


        permissionDto = new PermissionDto(name, null, end, grant, connectionId, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(2, violations.size());
        var violationMessages = violations.stream().map(ConstraintViolation::getMessage);
        assertThat(violationMessages).hasSameElementsAs(List.of("startTime mustn't be null.",
                "startTime and expirationTime mustn't be null."));


        permissionDto = new PermissionDto(name, start, null, grant, connectionId, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(2, violations.size());
        violationMessages = violations.stream().map(ConstraintViolation::getMessage);
        assertThat(violationMessages).hasSameElementsAs(List.of("expirationTime mustn't be null.",
                "startTime and expirationTime mustn't be null."));


        permissionDto = new PermissionDto(name, start, end, null, connectionId, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("grantTime mustn't be null.", first.getMessage());


        permissionDto = new PermissionDto(name, start, end, grant, null, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("connectionId mustn't be null or blank.", first.getMessage());


        permissionDto = new PermissionDto(name, start, end, grant, connectionId, null, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("At least one OBIS code needs to be requested.", first.getMessage());


        permissionDto = new PermissionDto(name, start, end, grant, connectionId, codes, null);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("kafkaStreamingConfig mustn't be null.", first.getMessage());
    }

    @Test
    void givenInvalidNestedInput_validation_fails() {
        streamingConfig = new KafkaStreamingConfig("", validDataTopic, validStatusTopic, validSubscribeTopic);

        var permissionDto = new PermissionDto(name, start, end, grant, connectionId, codes, streamingConfig);

        // validator also recursively validates any nested classes
        var violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("bootstrapServers mustn't be null or blank.", first.getMessage());
    }

    @Test
    void givenValidInput_asExpected() {
        var permissionDto = new PermissionDto(name, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permissionDto);
        assertEquals(0, violations.size());

        assertEquals(name, permissionDto.serviceName());
        assertEquals(start, permissionDto.startTime());
        assertEquals(end, permissionDto.expirationTime());
        assertEquals(grant, permissionDto.grantTime());
        assertEquals(connectionId, permissionDto.connectionId());
        assertThat(codes).hasSameElementsAs(permissionDto.requestedCodes());

        assertEquals(bootstrapServers, permissionDto.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, permissionDto.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, permissionDto.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, permissionDto.kafkaStreamingConfig().subscribeTopic());
    }
}
