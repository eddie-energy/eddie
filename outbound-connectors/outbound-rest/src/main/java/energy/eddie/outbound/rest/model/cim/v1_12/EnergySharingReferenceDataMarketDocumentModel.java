// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model.cim.v1_12;

import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity(name = "EnergySharingReferenceDataMarketDocumentModel")
@Table(name = "energy_sharing_reference_data_market_document", schema = "rest")
@SuppressWarnings("NullAway")
public class EnergySharingReferenceDataMarketDocumentModel implements ModelWithJsonPayload<ESRDMDEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final ESRDMDEnvelope payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public EnergySharingReferenceDataMarketDocumentModel(
            ZonedDateTime insertedAt,
            ESRDMDEnvelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public EnergySharingReferenceDataMarketDocumentModel(ESRDMDEnvelope payload) {
        this(null, payload);
    }

    protected EnergySharingReferenceDataMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public ESRDMDEnvelope payload() {
        return payload;
    }
}
