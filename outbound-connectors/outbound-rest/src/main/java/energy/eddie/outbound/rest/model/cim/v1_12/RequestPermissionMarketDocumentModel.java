// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model.cim.v1_12;

import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "request_permission_market_document_v1_12", schema = "rest")
@SuppressWarnings("NullAway")
public class RequestPermissionMarketDocumentModel implements ModelWithJsonPayload<RequestPermissionEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final RequestPermissionEnvelope payload;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("unused")
    private Long id;

    public RequestPermissionMarketDocumentModel(
            ZonedDateTime insertedAt,
            RequestPermissionEnvelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public RequestPermissionMarketDocumentModel(RequestPermissionEnvelope payload) {
        this(null, payload);
    }

    protected RequestPermissionMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public RequestPermissionEnvelope payload() {
        return payload;
    }
}
