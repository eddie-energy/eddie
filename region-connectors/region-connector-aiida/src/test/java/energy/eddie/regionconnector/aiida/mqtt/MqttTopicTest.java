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

        String expectedBase = "aiida/v1/" + permissionId + "/" + MqttTopicType.OUTBOUND_DATA.topicName();
        assertEquals(expectedBase, topic.baseTopic());
    }

    @Test
    void topicPattern_reflectsHasSuffix_forAllTopicTypes() {
        String permissionId = "perm-xyz";
        for (MqttTopicType type : MqttTopicType.values()) {
            MqttTopic topic = new MqttTopic("aiida/v1", permissionId, type);
            String expected = topic.baseTopic() + (type.hasSuffix() ? "/+" : "");
            assertEquals(expected, topic.topicPattern(), "Mismatch for type: " + type);
        }
    }

    @Test
    void extractPermissionIdFromTopic_matches_withAndWithoutSchema() throws Exception {
        String permissionId = "perm-42";
        MqttTopicType type = MqttTopicType.OUTBOUND_DATA;
        AiidaSchema schema = AiidaSchema.SMART_METER_P1_RAW;

        String base = "aiida/v1/" + permissionId + "/" + type.topicName();
        String withSchema = base + "/" + schema.topicName();

        // without schema segment (schema is optional in the pattern)
        assertEquals(permissionId, MqttTopic.extractPermissionIdFromTopic(base, type, schema));

        // with schema segment present
        assertEquals(permissionId, MqttTopic.extractPermissionIdFromTopic(withSchema, type, schema));
    }

    @Test
    void extractPermissionIdFromTopic_throws_onMismatch() {
        String badTopic = "aiida/v1/perm-42/data/other"; // wrong topic name segment
        assertThrows(MqttTopicException.class, () ->
                MqttTopic.extractPermissionIdFromTopic(badTopic, MqttTopicType.OUTBOUND_DATA, AiidaSchema.SMART_METER_P1_RAW)
        );

        String wrongPrefix = "wrongprefix/perm-42/" + MqttTopicType.OUTBOUND_DATA.topicName();
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
        assertEquals(topic.topicPattern(), acl.topic());
    }
}
