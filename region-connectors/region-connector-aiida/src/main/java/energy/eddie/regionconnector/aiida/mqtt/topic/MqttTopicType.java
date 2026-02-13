// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.topic;

import energy.eddie.api.agnostic.aiida.mqtt.MqttAclType;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;

public enum MqttTopicType {
    OUTBOUND_DATA("data/outbound", "/+", MqttAction.PUBLISH, MqttAclType.ALLOW),
    INBOUND_DATA("data/inbound", "/+", MqttAction.SUBSCRIBE, MqttAclType.ALLOW),
    STATUS("status", MqttAction.PUBLISH, MqttAclType.ALLOW),
    TERMINATION("termination", MqttAction.SUBSCRIBE, MqttAclType.ALLOW);

    private final String baseTopicName;
    private final String topicSuffix;
    private final MqttAction aiidaAclAction;
    private final MqttAclType aiidaAclType;

    MqttTopicType(String baseTopicName, MqttAction aiidaAclAction, MqttAclType aiidaAclType) {
        this(baseTopicName, "", aiidaAclAction, aiidaAclType);
    }

    MqttTopicType(String baseTopicName, String topicSuffix, MqttAction aiidaAclAction, MqttAclType aiidaAclType) {
        this.baseTopicName = baseTopicName;
        this.topicSuffix = topicSuffix;
        this.aiidaAclAction = aiidaAclAction;
        this.aiidaAclType = aiidaAclType;
    }

    public String baseTopicName() {
        return baseTopicName;
    }

    public String topicSuffix() {
        return topicSuffix;
    }

    public MqttAction aiidaAclAction() {
        return aiidaAclAction;
    }

    public MqttAclType aiidaAclType() {
        return aiidaAclType;
    }
}