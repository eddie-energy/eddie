package energy.eddie.aiida;

import energy.eddie.aiida.models.permission.KafkaStreamingConfig;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.containers.KafkaContainer;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {
    /**
     * Creates a KafkaConsumer that connects to the testcontainer of this testclass and uses the displayName of
     * the supplied testInfo as <i>group.id</i>.
     */
    public static KafkaConsumer<String, String> getKafkaConsumer(TestInfo testInfo, KafkaContainer kafka) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafka.getBootstrapServers());
        properties.put("group.id", testInfo.getDisplayName());
        // make sure to consume all records even if consumer subscribed after records have already been published
        properties.put("auto.offset.reset", "earliest");
        return new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer());
    }

    /**
     * Creates a KafkaStreamingConfig with unique topic names allowing that the KafkaContainer can be shared between
     * tests and to ensure no test method reuses the topic of another method.
     *
     * @param testInfo testInfo object of the test method
     */
    public static KafkaStreamingConfig getKafkaConfig(TestInfo testInfo, KafkaContainer kafka) {
        String prefix = testInfo.getDisplayName().substring(0, testInfo.getDisplayName().indexOf("("));
        var dataTopic = prefix + "_data";
        var statusTopic = prefix + "_status";
        var subscribeTopic = prefix + "_subscribe";
        return new KafkaStreamingConfig(kafka.getBootstrapServers(), dataTopic, statusTopic, subscribeTopic);
    }

    public static void verifyErrorLogStartsWith(String startString, LogCaptor logCaptor) {
        verifyErrorLogStartsWith(startString, logCaptor, null);
    }

    public static void verifyErrorLogStartsWith(String startString, LogCaptor logCaptor, Class<?> expectedExceptionClass) {
        verifyErrorLogStartsWith(startString, logCaptor, expectedExceptionClass, null);
    }

    public static void verifyErrorLogStartsWith(String startString, LogCaptor logCaptor, Class<?> expectedExceptionClass, String exceptionMessage) {
        var errorEvents = getErrorEvents(logCaptor);
        assertEquals(1, errorEvents.size());
        assertThat(errorEvents.get(0).getMessage()).startsWith(startString);

        if (expectedExceptionClass == null)
            return;

        var throwable = errorEvents.get(0).getThrowable();
        assertThat(throwable).isPresent();
        assertThat(errorEvents.get(0).getThrowable().orElseThrow())
                .isInstanceOf(expectedExceptionClass);

        if (exceptionMessage != null)
            assertThat(errorEvents.get(0).getThrowable().orElseThrow())
                    .hasMessage(exceptionMessage);
    }

    private static List<LogEvent> getErrorEvents(LogCaptor logCaptor) {
        return logCaptor.getLogEvents().stream().filter(logEvent -> logEvent.getLevel().equals("ERROR")).toList();
    }
}
