// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.api.agnostic.aiida.mqtt.MqttAclType;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;
import energy.eddie.dataneeds.needs.aiida.AiidaSchema;
import energy.eddie.regionconnector.aiida.exceptions.MqttTopicException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttTopicTest {

    @Test
    void of_createsTopic_and_baseTopicIsCorrect() {
        String permissionId = "perm-123";
        MqttTopic topic = MqttTopic.of(permissionId, MqttTopicType.OUTBOUND_DATA);

        assertEquals("aiida/v1/perm-123/data/outbound", topic.baseTopic());
    }

    @Test
    void eddieTopic_withSuffix_publish() {
        String permissionId = "perm-xyz";
        MqttTopic topic = MqttTopic.of(permissionId, MqttTopicType.OUTBOUND_DATA);

        assertEquals("aiida/v1/perm-xyz/data/outbound/+", topic.eddieTopic());
    }

    @Test
    void eddieTopic_withSuffix_subscribe() {
        String permissionId = "perm-xyz";
        MqttTopic topic = MqttTopic.of(permissionId, MqttTopicType.INBOUND_DATA);

        assertEquals("aiida/v1/perm-xyz/data/inbound", topic.eddieTopic());
    }

    @Test
    void eddieTopic_withoutSuffix() {
        String permissionId = "perm-xyz";
        MqttTopic topic = MqttTopic.of(permissionId, MqttTopicType.STATUS);

        assertEquals("aiida/v1/perm-xyz/status", topic.eddieTopic());
    }

    @Test
    void aiidaTopic_withSuffix_subscribe() {
        String permissionId = "perm-abc";
        MqttTopic topic = MqttTopic.of(permissionId, MqttTopicType.INBOUND_DATA);

        assertEquals("aiida/v1/perm-abc/data/inbound/+", topic.aiidaTopic());
    }

    @Test
    void aiidaTopic_withSuffix_publish() {
        String permissionId = "perm-abc";
        MqttTopic topic = MqttTopic.of(permissionId, MqttTopicType.OUTBOUND_DATA);

        assertEquals("aiida/v1/perm-abc/data/outbound", topic.aiidaTopic());
    }

    @Test
    void aiidaTopic_withoutSuffix() {
        String permissionId = "perm-abc";
        MqttTopic topic = MqttTopic.of(permissionId, MqttTopicType.TERMINATION);

        assertEquals("aiida/v1/perm-abc/termination", topic.aiidaTopic());
    }

    @Test
    void extractPermissionIdFromTopic_matches_withSchema() throws Exception {
        String permissionId = "perm-42";
        MqttTopicType type = MqttTopicType.OUTBOUND_DATA;
        AiidaSchema schema = AiidaSchema.SMART_METER_P1_RAW;

        String base = "aiida/v1/" + permissionId + "/" + type.baseTopicName() + "/" + schema.topicName();

        assertEquals(permissionId, MqttTopic.extractPermissionIdFromTopic(base, type, schema));
    }

    @Test
    void extractPermissionIdFromTopic_matches_withoutSchema() throws Exception {
        String permissionId = "perm-42";
        MqttTopicType type = MqttTopicType.STATUS;

        String base = "aiida/v1/" + permissionId + "/" + type.baseTopicName();

        assertEquals(permissionId, MqttTopic.extractPermissionIdFromTopic(base, type, null));
    }

    @Test
    void extractPermissionIdFromTopic_throws_onMismatch() {
        String badTopic = "aiida/v1/perm-42/data/other"; // wrong topic name segment
        assertThrows(MqttTopicException.class, () ->
                MqttTopic.extractPermissionIdFromTopic(badTopic, MqttTopicType.OUTBOUND_DATA, AiidaSchema.SMART_METER_P1_RAW)
        );

        String wrongPrefix = "wrongprefix/perm-42/" + MqttTopicType.OUTBOUND_DATA.baseTopicName();
        assertThrows(MqttTopicException.class, () ->
                MqttTopic.extractPermissionIdFromTopic(wrongPrefix, MqttTopicType.OUTBOUND_DATA, AiidaSchema.SMART_METER_P1_RAW)
        );
    }

    @Test
    void aiidaAcl_createsCorrectAcl() {
        var topic = MqttTopic.of("perm-99", MqttTopicType.INBOUND_DATA);
        var acl = topic.aiidaAcl("user1");

        assertEquals("user1", acl.username());
        assertEquals(MqttAction.SUBSCRIBE, acl.action());
        assertEquals(MqttAclType.ALLOW, acl.aclType());
        assertEquals("aiida/v1/perm-99/data/inbound/+", acl.topic());
    }
}
