// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model.cim.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity(name = "AcknowledgementMarketDocumentModel")
@Table(name = "acknowledgement_market_document", schema = "rest")
@SuppressWarnings("NullAway")
public class AcknowledgementMarketDocumentModel implements ModelWithJsonPayload<AcknowledgementEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final AcknowledgementEnvelope payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public AcknowledgementMarketDocumentModel(
            ZonedDateTime insertedAt,
            AcknowledgementEnvelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public AcknowledgementMarketDocumentModel(AcknowledgementEnvelope payload) {
        this(null, payload);
    }

    protected AcknowledgementMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public AcknowledgementEnvelope payload() {
        return payload;
    }
}
