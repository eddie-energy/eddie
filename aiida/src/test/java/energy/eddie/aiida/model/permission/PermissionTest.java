package energy.eddie.aiida.model.permission;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {
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

        var permission = new Permission(name, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permission);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("expirationTime has to be after startTime.", first.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void givenEmptyConnectionIdOrServiceName_validation_fails(String str) {
        var permission = new Permission(str, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permission);
        assertEquals(1, violations.size());

        permission = new Permission(name, start, end, grant, str, codes, streamingConfig);

        violations = validator.validate(permission);
        assertEquals(1, violations.size());
    }

    @Test
    void givenNull_throws() {
        assertThrows(NullPointerException.class, () -> new Permission(null, start, end, grant, connectionId, codes, streamingConfig));
        assertThrows(NullPointerException.class, () -> new Permission(name, null, end, grant, connectionId, codes, streamingConfig));
        assertThrows(NullPointerException.class, () -> new Permission(name, start, null, grant, connectionId, codes, streamingConfig));
        assertThrows(NullPointerException.class, () -> new Permission(name, start, end, null, connectionId, codes, streamingConfig));
        assertThrows(NullPointerException.class, () -> new Permission(name, start, end, grant, null, codes, streamingConfig));
        assertThrows(NullPointerException.class, () -> new Permission(name, start, end, grant, connectionId, null, streamingConfig));
        assertThrows(NullPointerException.class, () -> new Permission(name, start, end, grant, connectionId, codes, null));

        var permission = new Permission(name, start, end, grant, connectionId, codes, streamingConfig);
        assertThrows(NullPointerException.class, () -> permission.updateStatus(null));
        assertThrows(NullPointerException.class, () -> permission.terminateTime(null));
    }

    @Test
    void givenInvalidNestedInput_validation_fails() {
        streamingConfig = new KafkaStreamingConfig("", validDataTopic, validStatusTopic, validSubscribeTopic);

        var permission = new Permission(name, start, end, grant, connectionId, codes, streamingConfig);

        // validator also recursively validates any nested classes
        var violations = validator.validate(permission);
        assertEquals(1, violations.size());
        var first = violations.iterator().next();
        assertEquals("bootstrapServers mustn't be null or blank.", first.getMessage());
    }

    @Test
    void givenTerminateTimeBeforeGrantTime_terminationTime_throws() {
        var permission = new Permission(name, start, end, grant, connectionId, codes, streamingConfig);
        var terminate = start.minusSeconds(1000);

        assertThrows(IllegalArgumentException.class, () -> permission.terminateTime(terminate));
    }

    @Test
    void givenValidInput_asExpected() {
        var permission = new Permission(name, start, end, grant, connectionId, codes, streamingConfig);

        var violations = validator.validate(permission);
        assertEquals(0, violations.size());

        assertEquals(name, permission.serviceName());
        assertEquals(start, permission.startTime());
        assertEquals(end, permission.expirationTime());
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
        var permission = new Permission(name, start, end, grant, connectionId, codes, streamingConfig);

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
        assertEquals(end, permission.expirationTime());
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
