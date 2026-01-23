// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model.cim.v0_82;

import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "permission_market_document", schema = "rest")
@SuppressWarnings("NullAway")
public class PermissionMarketDocumentModel implements ModelWithJsonPayload<PermissionEnvelope> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final PermissionEnvelope payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public PermissionMarketDocumentModel(
            ZonedDateTime insertedAt,
            PermissionEnvelope payload
    ) {
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public PermissionMarketDocumentModel(PermissionEnvelope payload) {
        this(null, payload);
    }

    protected PermissionMarketDocumentModel() {
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public PermissionEnvelope payload() {
        return payload;
    }
}
