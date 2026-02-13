// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.acl;

import energy.eddie.api.agnostic.aiida.mqtt.MqttAclType;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "aiida_mqtt_acl", schema = AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID)
public class MqttAcl {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private final String id;
    @Column(name = "username", nullable = false)
    private final String username;
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private final MqttAction action;
    @Enumerated(EnumType.STRING)
    @Column(name = "acl_type", nullable = false)
    private final MqttAclType aclType;
    @Column(name = "topic", nullable = false)
    private final String topic;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private final Instant createdAt;

    @SuppressWarnings("NullAway")
    protected MqttAcl() {
        this.id = null;
        this.username = null;
        this.action = null;
        this.aclType = null;
        this.topic = null;
        this.createdAt = null;
    }

    @SuppressWarnings("NullAway")
    public MqttAcl(
            String username,
            MqttAction action,
            MqttAclType aclType,
            String topic
    ) {
        this.username = username;
        this.action = action;
        this.aclType = aclType;
        this.topic = topic;

        this.id = null;
        this.createdAt = null;
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public MqttAction action() {
        return action;
    }

    public MqttAclType aclType() {
        return aclType;
    }

    public String topic() {
        return topic;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
