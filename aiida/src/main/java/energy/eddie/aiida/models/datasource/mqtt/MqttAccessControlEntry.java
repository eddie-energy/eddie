// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAclType;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "data_source_mqtt_acl")
public class MqttAccessControlEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @JsonIgnore
    @SuppressWarnings("unused")
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonIgnore
    @SuppressWarnings("unused")
    private MqttAction action;

    @Column(name = "acl_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonIgnore
    @SuppressWarnings("unused")
    private MqttAclType aclType;

    @Column(nullable = false)
    @JsonIgnore
    private String topic;

    @Column(nullable = false)
    @JsonIgnore
    @SuppressWarnings("unused")
    private String username;

    @SuppressWarnings("NullAway")
    protected MqttAccessControlEntry() {}

    @SuppressWarnings("NullAway")
    public MqttAccessControlEntry(UUID dataSourceId, String topic) {
        this.action = MqttAction.ALL;
        this.aclType = MqttAclType.ALLOW;
        this.topic = topic;
        this.username = dataSourceId.toString();
    }

    @SuppressWarnings("NullAway")
    public MqttAccessControlEntry(String username, String topic) {
        this.action = MqttAction.ALL;
        this.aclType = MqttAclType.ALLOW;
        this.topic = topic;
        this.username = username;
    }

    public String topic() {
        return topic;
    }
}