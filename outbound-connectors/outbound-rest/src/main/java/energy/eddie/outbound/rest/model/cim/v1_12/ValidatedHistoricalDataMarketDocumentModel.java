// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model.cim.v1_12;

import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity(name = "ValidatedHistoricalDataMarketDocumentModelV1_12")
@Table(name = "validated_historical_data_marked_document_v1_12", schema = "rest")
@SuppressWarnings("NullAway")
public class ValidatedHistoricalDataMarketDocumentModel implements ModelWithJsonPayload<Envelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final Envelope payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public ValidatedHistoricalDataMarketDocumentModel(
            ZonedDateTime insertedAt,
            Envelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public ValidatedHistoricalDataMarketDocumentModel(Envelope payload) {
        this(null, payload);
    }

    protected ValidatedHistoricalDataMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public Envelope payload() {
        return payload;
    }
}
