package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.api.agnostic.aiida.mqtt.MqttAclType;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;

public enum MqttTopicType {
    OUTBOUND_DATA("data/outbound", true, MqttAction.PUBLISH, MqttAclType.ALLOW),
    INBOUND_DATA("data/inbound", true, MqttAction.SUBSCRIBE, MqttAclType.ALLOW),
    STATUS("status", false, MqttAction.PUBLISH, MqttAclType.ALLOW),
    TERMINATION("termination", false, MqttAction.SUBSCRIBE, MqttAclType.ALLOW);

    private final String topicName;
    private final boolean hasSuffix;
    private final MqttAction aiidaAclAction;
    private final MqttAclType aiidaAclType;

    MqttTopicType(String topicName, boolean hasSuffix, MqttAction aiidaAclAction, MqttAclType aiidaAclType) {
        this.topicName = topicName;
        this.hasSuffix = hasSuffix;
        this.aiidaAclAction = aiidaAclAction;
        this.aiidaAclType = aiidaAclType;
    }

    public String topicName() {
        return topicName;
    }

    public boolean hasSuffix() {
        return hasSuffix;
    }

    public MqttAction aiidaAclAction() {
        return aiidaAclAction;
    }

    public MqttAclType aiidaAclType() {
        return aiidaAclType;
    }
}