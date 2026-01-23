// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.model;

import energy.eddie.api.agnostic.RawDataMessage;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;

@Entity
@Table(name = "raw_data_message", schema = "rest")
@SuppressWarnings("NullAway")
public class RawDataMessageModel implements ModelWithJsonPayload<RawDataMessage> {
    @Column(name = "inserted_at", nullable = false, insertable = false, updatable = false)
    @SuppressWarnings("unused")
    private final ZonedDateTime insertedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private final RawDataMessage payload;
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public RawDataMessageModel(
            ZonedDateTime insertedAt,
            RawDataMessage payload
    ) {
        this.id = null;
        this.insertedAt = insertedAt;
        this.payload = payload;
    }

    public RawDataMessageModel(RawDataMessage payload) {
        this(null, payload);
    }

    protected RawDataMessageModel() {
        this.id = null;
        this.insertedAt = null;
        this.payload = null;
    }

    @Override
    public RawDataMessage payload() {
        return payload;
    }
}
