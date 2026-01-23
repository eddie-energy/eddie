// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MqttTopicTypeTest {
    @Test
    void testTopicType() {
        assertEquals("data/outbound", MqttTopicType.OUTBOUND_DATA.baseTopicName());
        assertEquals("data/inbound", MqttTopicType.INBOUND_DATA.baseTopicName());
        assertEquals("status", MqttTopicType.STATUS.baseTopicName());
        assertEquals("termination", MqttTopicType.TERMINATION.baseTopicName());
    }
}
