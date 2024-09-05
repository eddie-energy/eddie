package energy.eddie.regionconnector.aiida.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicTypeTest {
    @Test
    void testTopicType() {
        assertEquals("data", TopicType.DATA.topicName());
        assertEquals("status", TopicType.STATUS.topicName());
        assertEquals("termination", TopicType.TERMINATION.topicName());
    }
}
