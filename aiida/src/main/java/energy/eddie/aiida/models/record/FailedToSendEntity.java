// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "failed_to_send_entity")
public class FailedToSendEntity {
    @Id
    @SequenceGenerator(name = "failed_to_send_entity_seq", sequenceName = "failed_to_send_entity_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failed_to_send_entity_seq")
    private Long id;
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;
    @Column(name = "topic", nullable = false)
    private String topic;
    @Column(name = "json_value", nullable = false)
    private byte[] json;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @SuppressWarnings("NullAway.Init")
    protected FailedToSendEntity() {
    }

    @SuppressWarnings("NullAway.Init")
    public FailedToSendEntity(UUID permissionId, String topic, byte[] json) {
        this.permissionId = permissionId;
        this.topic = topic;
        this.json = json;
    }

    public Long id() {
        return id;
    }

    public UUID permissionId() {
        return permissionId;
    }

    public String topic() {
        return topic;
    }

    public byte[] json() {
        return json;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
