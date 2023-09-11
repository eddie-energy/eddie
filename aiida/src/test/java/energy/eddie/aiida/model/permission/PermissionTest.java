package energy.eddie.aiida.model.permission;

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
import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {
    private Validator validator;
    private Instant start;
    private Instant expiration;
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
        expiration = start.plusSeconds(5000);

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
        expiration = start.minusSeconds(1000);

        var permission = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permission);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("expirationTime has to be after startTime.", first.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void givenEmptyConnectionIdOrServiceName_validation_fails(String str) {
        var permission = new Permission(str, start, expiration, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permission);
        assertEquals(1, violations.size());

        permission = new Permission(name, start, expiration, grant, str, codes, streamingConfig);

        violations = validator.validate(permission);
        assertEquals(1, violations.size());
    }

    @Test
    void givenNull_validation_fails() {
        var permission = new Permission(null, start, expiration, grant, connectionId, codes, streamingConfig);
        var violations = validator.validate(permission);
        assertEquals(1, violations.size());
        assertEquals("serviceName mustn't be null or blank.", violations.iterator().next().getMessage());

        permission = new Permission(name, null, expiration, grant, connectionId, codes, streamingConfig);
        violations = validator.validate(permission);
        assertEquals(2, violations.size());
        var violationMessages = violations.stream().map(ConstraintViolation::getMessage);
        assertThat(violationMessages).hasSameElementsAs(List.of("startTime mustn't be null.",
                "startTime and expirationTime mustn't be null."));

        permission = new Permission(name, start, null, grant, connectionId, codes, streamingConfig);
        violations = validator.validate(permission);
        assertEquals(2, violations.size());
        violationMessages = violations.stream().map(ConstraintViolation::getMessage);
        assertThat(violationMessages).hasSameElementsAs(List.of("expirationTime mustn't be null.",
                "startTime and expirationTime mustn't be null."));

        permission = new Permission(name, start, expiration, null, connectionId, codes, streamingConfig);
        violations = validator.validate(permission);
        assertEquals(1, violations.size());
        assertEquals("grantTime mustn't be null.", violations.iterator().next().getMessage());

        permission = new Permission(name, start, expiration, grant, null, codes, streamingConfig);
        violations = validator.validate(permission);
        assertEquals(1, violations.size());
        assertEquals("connectionId mustn't be null or blank.", violations.iterator().next().getMessage());

        permission = new Permission(name, start, expiration, grant, connectionId, null, streamingConfig);
        violations = validator.validate(permission);
        assertEquals(1, violations.size());
        assertEquals("At least one OBIS code needs to be requested.", violations.iterator().next().getMessage());

        permission = new Permission(name, start, expiration, grant, connectionId, codes, null);
        violations = validator.validate(permission);
        assertEquals(1, violations.size());
        assertEquals("kafkaStreamingConfig mustn't be null.", violations.iterator().next().getMessage());
    }

    @Test
    void givenNull_updateStatus_throws() {
        var permission = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);
        assertThrows(NullPointerException.class, () -> permission.updateStatus(null));
    }

    @Test
    void givenNull_terminateTime_throws() {
        var permission = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);
        assertThrows(NullPointerException.class, () -> permission.terminateTime(null));
    }

    @Test
    void givenInvalidNestedInput_validation_fails() {
        streamingConfig = new KafkaStreamingConfig("", validDataTopic, validStatusTopic, validSubscribeTopic);

        var permission = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);

        // validator also recursively validates any nested classes
        var violations = validator.validate(permission);
        System.out.println(violations);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("bootstrapServers mustn't be null or blank.", first.getMessage());
    }

    @Test
    void givenTerminateTimeBeforeGrantTime_terminationTime_throws() {
        var permission = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);
        var terminate = start.minusSeconds(1000);

        assertThrows(IllegalArgumentException.class, () -> permission.terminateTime(terminate));
    }

    @Test
    void givenValidInput_asExpected() {
        var permission = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permission);
        assertEquals(0, violations.size());

        assertEquals(name, permission.serviceName());
        assertEquals(start, permission.startTime());
        assertEquals(expiration, permission.expirationTime());
        assertEquals(grant, permission.grantTime());
        assertEquals(connectionId, permission.connectionId());

        // permissionId is generated by database, therefore null until object is persisted in database
        assertNull(permission.permissionId());

        assertNull(permission.terminateTime());
        assertEquals(PermissionStatus.ACCEPTED, permission.status());

        assertThat(codes).hasSameElementsAs(permission.requestedCodes());
        assertEquals(bootstrapServers, permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, permission.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, permission.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, permission.kafkaStreamingConfig().subscribeTopic());
    }

    @Test
    void givenValidTerminationTime_asExpected() {
        var permission = new Permission(name, start, expiration, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permission);
        assertEquals(0, violations.size());

        var terminate = start.plusSeconds(1000);

        permission.terminateTime(terminate);
        permission.updateStatus(PermissionStatus.TERMINATED);

        assertEquals(terminate, permission.terminateTime());
        assertEquals(PermissionStatus.TERMINATED, permission.status());

        // no other fields should have been modified
        assertEquals(name, permission.serviceName());
        assertEquals(start, permission.startTime());
        assertEquals(expiration, permission.expirationTime());
        assertEquals(grant, permission.grantTime());
        assertEquals(connectionId, permission.connectionId());
        // permissionId is generated by database, therefore null until object is persisted in database
        assertNull(permission.permissionId());
        assertThat(codes).hasSameElementsAs(permission.requestedCodes());
        assertEquals(bootstrapServers, permission.kafkaStreamingConfig().bootstrapServers());
        assertEquals(validDataTopic, permission.kafkaStreamingConfig().dataTopic());
        assertEquals(validStatusTopic, permission.kafkaStreamingConfig().statusTopic());
        assertEquals(validSubscribeTopic, permission.kafkaStreamingConfig().subscribeTopic());
    }
}
