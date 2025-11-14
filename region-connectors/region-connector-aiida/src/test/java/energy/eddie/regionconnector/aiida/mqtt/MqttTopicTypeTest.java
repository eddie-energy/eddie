package energy.eddie.regionconnector.aiida.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MqttTopicTypeTest {
    @Test
    void testTopicType() {
        assertEquals("data/outbound", MqttTopicType.OUTBOUND_DATA.topicName());
        assertEquals("data/inbound", MqttTopicType.INBOUND_DATA.topicName());
        assertEquals("status", MqttTopicType.STATUS.topicName());
        assertEquals("termination", MqttTopicType.TERMINATION.topicName());
    }
}
