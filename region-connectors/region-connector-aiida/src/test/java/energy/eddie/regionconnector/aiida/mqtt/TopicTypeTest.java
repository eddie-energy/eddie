package energy.eddie.regionconnector.aiida.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopicTypeTest {
    @Test
    void testTopicType() {
        assertEquals("data/outbound", TopicType.OUTBOUND_DATA.topicName());
        assertEquals("data/inbound", TopicType.INBOUND_DATA.topicName());
        assertEquals("status", TopicType.STATUS.topicName());
        assertEquals("termination", TopicType.TERMINATION.topicName());
    }
}
