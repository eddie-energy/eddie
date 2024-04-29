package energy.eddie.regionconnector.aiida.config;

import org.apache.kafka.common.errors.InvalidTopicException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlainAiidaConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {"Öüasd", "", " ", "#", "inval*d", "In/Valid", "In Val Id"})
    void givenAnyInvalidKafkaTopic_throwsException(String invalidTopic) {
        var valid = "ValidTopicName";

        assertThrows(InvalidTopicException.class,
                     () -> new PlainAiidaConfiguration("localhost:9093",
                                                       invalidTopic,
                                                       valid,
                                                       valid,
                                                       "customerId",
                                                       4,
                                                       "http://localhost:8080"));
        assertThrows(InvalidTopicException.class,
                     () -> new PlainAiidaConfiguration("localhost:9093",
                                                       valid,
                                                       invalidTopic,
                                                       valid,
                                                       "customerId",
                                                       4,
                                                       "http://localhost:8080"));
        assertThrows(InvalidTopicException.class,
                     () -> new PlainAiidaConfiguration("localhost:9093",
                                                       valid,
                                                       valid,
                                                       invalidTopic,
                                                       "customerId",
                                                       4,
                                                       "http://localhost:8080"));
    }

    @Test
    void givenValidTopic_doesNotThrow() {
        var valid = "ValidTopicName";
        assertDoesNotThrow(() -> new PlainAiidaConfiguration("localhost:9093",
                                                             valid,
                                                             valid,
                                                             valid,
                                                             "customerId",
                                                             4,
                                                             "http://localhost:8080"));
    }
}
