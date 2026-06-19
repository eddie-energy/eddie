// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model.cim.v1_12;

import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "min_max_envelope_market_document", schema = "rest")
@SuppressWarnings("NullAway")
public class MinMaxEnvelopeMarketDocumentModel implements ModelWithJsonPayload<RECMMOEEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final RECMMOEEnvelope payload;

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public MinMaxEnvelopeMarketDocumentModel(ZonedDateTime insertedAt, RECMMOEEnvelope payload) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public MinMaxEnvelopeMarketDocumentModel(RECMMOEEnvelope payload) {
        this(null, payload);
    }

    protected MinMaxEnvelopeMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public RECMMOEEnvelope payload() {
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
        if (!(o instanceof MinMaxEnvelopeMarketDocumentModel that)) return false;

        return Objects.equals(insertedAt, that.insertedAt)
               && Objects.equals(payload, that.payload)
               && Objects.equals(id, that.id);
    }
}
