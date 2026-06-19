// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model;

import energy.eddie.cim.agnostic.OpaqueEnvelope;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "opaque_envelope", schema = "rest")
@SuppressWarnings("NullAway")
public class OpaqueEnvelopeModel implements ModelWithJsonPayload<OpaqueEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final OpaqueEnvelope payload;

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public OpaqueEnvelopeModel(ZonedDateTime insertedAt, OpaqueEnvelope payload) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public OpaqueEnvelopeModel(OpaqueEnvelope payload) {
        this(null, payload);
    }

    protected OpaqueEnvelopeModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public OpaqueEnvelope payload() {
        return payload;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(insertedAt);
        result = 31 * result + Objects.hashCode(payload);
        result = 31 * result + Objects.hashCode(id);
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof OpaqueEnvelopeModel that)) return false;

        return Objects.equals(insertedAt, that.insertedAt)
               && Objects.equals(payload, that.payload)
               && Objects.equals(id, that.id);
    }
}
