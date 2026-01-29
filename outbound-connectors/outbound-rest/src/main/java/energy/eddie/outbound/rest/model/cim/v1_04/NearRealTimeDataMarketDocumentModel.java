// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model.cim.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "near_real_time_data_marked_document", schema = "rest")
@SuppressWarnings("NullAway")
public class NearRealTimeDataMarketDocumentModel implements ModelWithJsonPayload<RTDEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final RTDEnvelope payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public NearRealTimeDataMarketDocumentModel(
            ZonedDateTime insertedAt,
            RTDEnvelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public NearRealTimeDataMarketDocumentModel(RTDEnvelope payload) {
        this(null, payload);
    }

    protected NearRealTimeDataMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public RTDEnvelope payload() {
        return payload;
    }
}
