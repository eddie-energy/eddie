package energy.eddie.aiida.models.permission;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaStreamingConfigTest {
    private Validator validator;
    private ValidatorFactory validatorFactory;
    private String bootstrapServers;
    private String dataTopic;
    private String statusTopic;
    private String subscribeTopic;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        bootstrapServers = "localhost:9092";
        dataTopic = "ValidPublishTopic";
        statusTopic = "ValidStatusTopic";
        subscribeTopic = "ValidSubscribeTopic";
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void givenNull_validation_fails() {
        var kafkaConfig = new KafkaStreamingConfig(null, dataTopic, statusTopic, subscribeTopic);

        var violations = validator.validate(kafkaConfig);
        assertEquals(1, violations.size());
        assertEquals("bootstrapServers must not be null or blank.", violations.iterator().next().getMessage());

        kafkaConfig = new KafkaStreamingConfig(bootstrapServers, null, statusTopic, subscribeTopic);
        violations = validator.validate(kafkaConfig);
        for (ConstraintViolation<KafkaStreamingConfig> violation : violations) {
            System.out.println(violation.getMessage());
        }
        assertEquals(1, violations.size());
        assertEquals("dataTopic must not be null or blank.", violations.iterator().next().getMessage());

        kafkaConfig = new KafkaStreamingConfig(bootstrapServers, dataTopic, null, subscribeTopic);
        violations = validator.validate(kafkaConfig);
        assertEquals(1, violations.size());
        assertEquals("statusTopic must not be null or blank.", violations.iterator().next().getMessage());

        kafkaConfig = new KafkaStreamingConfig(bootstrapServers, dataTopic, statusTopic, null);
        violations = validator.validate(kafkaConfig);
        assertEquals(1, violations.size());
        assertEquals("subscribeTopic must not be null or blank.", violations.iterator().next().getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void givenEmptyOrBlankString_validation_fails(String str) {
        var kafkaConfig = new KafkaStreamingConfig(str, dataTopic, statusTopic, subscribeTopic);

        var violations = validator.validate(kafkaConfig);
        assertEquals(1, violations.size());

        kafkaConfig = new KafkaStreamingConfig(bootstrapServers, str, statusTopic, subscribeTopic);
        violations = validator.validate(kafkaConfig);
        assertEquals(1, violations.size());

        kafkaConfig = new KafkaStreamingConfig(bootstrapServers, dataTopic, str, subscribeTopic);
        violations = validator.validate(kafkaConfig);
        assertEquals(1, violations.size());

        kafkaConfig = new KafkaStreamingConfig(bootstrapServers, dataTopic, statusTopic, str);
        violations = validator.validate(kafkaConfig);
        assertEquals(1, violations.size());
    }

    @Test
    void givenValidInput_asExpected() {
        var kafkaConfig = new KafkaStreamingConfig(bootstrapServers, dataTopic, statusTopic, subscribeTopic);

        assertEquals(bootstrapServers, kafkaConfig.bootstrapServers());
        assertEquals(dataTopic, kafkaConfig.dataTopic());
        assertEquals(statusTopic, kafkaConfig.statusTopic());
        assertEquals(subscribeTopic, kafkaConfig.subscribeTopic());
    }
}