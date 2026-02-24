// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.topic;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;
import energy.eddie.regionconnector.aiida.mqtt.acl.MqttAcl;

public record MqttTopic(
        String prefix,
        String permissionId,
        MqttTopicType topicType
) {
    public static final String DELIMITER = "/";
    public static final String DEFAULT_PREFIX = "aiida/v1";
    public static final int MESSAGE_VERSION_LENGTH = DEFAULT_PREFIX.length();
    public static final int PERMISSION_ID_LENGTH = 36;
    public static final int DELIMITER_LENGTH = DELIMITER.length();

    public static MqttTopic of(String permissionId, MqttTopicType topicType) {
        return new MqttTopic(DEFAULT_PREFIX, permissionId, topicType);
    }

    public String eddieTopic() {
        return buildTopic(MqttAction.PUBLISH);
    }

    public String aiidaTopic() {
        return buildTopic(MqttAction.SUBSCRIBE);
    }

    public String baseTopic() {
        return String.join(DELIMITER, prefix, permissionId, topicType.baseTopicName());
    }

    public String schemaTopic(AiidaSchema schema) {
        return schema.buildTopicPath(baseTopic());
    }

    public MqttAcl aiidaAcl(String username) {
        return new MqttAcl(
                username,
                topicType.aiidaAclAction(),
                topicType.aiidaAclType(),
                baseTopic() + topicType.topicSuffix()
        );
    }

    private String buildTopic(MqttAction requiredAction) {
        var suffix = (topicType.aiidaAclAction() == requiredAction)
                ? topicType.topicSuffix()
                : "";

        return baseTopic() + suffix;
    }
}
