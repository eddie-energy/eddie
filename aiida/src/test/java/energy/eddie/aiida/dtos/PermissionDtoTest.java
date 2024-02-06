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
    private String permissionId;
    private String name;
    private String connectionId;
    private String dataNeedId;
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
        permissionId = "34c149f5-8e50-487f-ab08-28c0a164f440";
        name = "My Test Service";
        connectionId = "RandomId";
        dataNeedId = "DataNeed";
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

        var permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permissionDto);
        System.out.println(violations);
        assertEquals(2, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("expirationTime has to be after startTime.",
                "expirationTime must not lie in the past."));
    }

    @Test
    void givenExpirationTimeInPast_validation_fails() {
        start = Instant.now().minusSeconds(1000);
        end = Instant.now().minusSeconds(500);

        var permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permissionDto);
        System.out.println(violations);
        assertEquals(1, violations.size());
        List<String> list = violations.stream().map(ConstraintViolation::getMessage).toList();
        assertThat(list).hasSameElementsAs(List.of("expirationTime must not lie in the past."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void givenEmptyConnectionIdOrServiceNameOrDataNeedId_validation_fails(String str) {
        var permissionDto = new PermissionDto(permissionId, str, dataNeedId, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());

        permissionDto = new PermissionDto(permissionId, name, str, start, end, grant, connectionId, codes, streamingConfig);

        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());


        permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, str, codes, streamingConfig);

        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
    }

    @Test
    void givenNull_validation_fails() {
        PermissionDto permissionDto = new PermissionDto(null, name, dataNeedId, start, end, grant, connectionId, codes, streamingConfig);
        var violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("must not be null.", first.getMessage());
        assertEquals("permissionId", violations.iterator().next().getPropertyPath().iterator().next().getName());

        permissionDto = new PermissionDto(permissionId, null, dataNeedId, start, end, grant, connectionId, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("must not be null or blank.", first.getMessage());
        assertEquals("serviceName", violations.iterator().next().getPropertyPath().iterator().next().getName());

        permissionDto = new PermissionDto(permissionId, name, null, start, end, grant, connectionId, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("must not be null or blank.", first.getMessage());
        assertEquals("dataNeedId", violations.iterator().next().getPropertyPath().iterator().next().getName());

        permissionDto = new PermissionDto(permissionId, name, dataNeedId, null, end, grant, connectionId, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(2, violations.size());
        var violationMessages = violations.stream().map(ConstraintViolation::getMessage);
        assertThat(violationMessages).hasSameElementsAs(List.of("must not be null.",
                "startTime and expirationTime must not be null."));
        var fieldNames = violations.stream().map(violation -> violation.getPropertyPath().iterator().next().getName()).toList();
        assertThat(fieldNames).contains("startTime");

        permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, null, grant, connectionId, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(3, violations.size());
        violationMessages = violations.stream().map(ConstraintViolation::getMessage);
        assertThat(violationMessages).hasSameElementsAs(List.of("expirationTime must not be null.",
                "must not be null.",
                "startTime and expirationTime must not be null."));

        permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, null, connectionId, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("must not be null.", first.getMessage());
        fieldNames = violations.stream().map(violation -> violation.getPropertyPath().iterator().next().getName()).toList();
        assertThat(fieldNames).contains("grantTime");


        permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, null, codes, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("must not be null or blank.", first.getMessage());
        assertEquals("connectionId", violations.iterator().next().getPropertyPath().iterator().next().getName());


        permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, connectionId, null, streamingConfig);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("At least one OBIS code needs to be requested.", first.getMessage());
        assertEquals("requestedCodes", violations.iterator().next().getPropertyPath().iterator().next().getName());


        permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, connectionId, codes, null);
        violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        first = violations.iterator().next();
        assertEquals("must not be null.", first.getMessage());
        assertEquals("kafkaStreamingConfig", violations.iterator().next().getPropertyPath().iterator().next().getName());
    }

    @Test
    void givenInvalidNestedInput_validation_fails() {
        streamingConfig = new KafkaStreamingConfig("", validDataTopic, validStatusTopic, validSubscribeTopic);

        var permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, connectionId, codes, streamingConfig);

        // validator also recursively validates any nested classes
        var violations = validator.validate(permissionDto);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("must not be null or blank.", first.getMessage());
    }

    @Test
    void givenValidInput_asExpected() {
        var permissionDto = new PermissionDto(permissionId, name, dataNeedId, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permissionDto);
        assertEquals(0, violations.size());

        assertEquals(permissionId, permissionDto.permissionId());
        assertEquals(name, permissionDto.serviceName());
        assertEquals(dataNeedId, permissionDto.dataNeedId());
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
