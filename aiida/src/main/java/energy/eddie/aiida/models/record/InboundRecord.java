// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;

@Entity
public class InboundRecord extends AbstractDataRecord {

    @JsonProperty
    @Enumerated(EnumType.STRING)
    protected AiidaSchema schema;

    @Column(nullable = false)
    @JsonProperty
    private String payload;

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected InboundRecord() {
    }

    public InboundRecord(
            Instant timestamp,
            InboundDataSource dataSource,
            AiidaSchema schema,
            String payload
    ) {
        super(timestamp, dataSource);
        this.schema = schema;
        this.payload = payload;
    }
    public AiidaSchema schema() {
        return schema;
    }

    public String payload() {
        return payload;
    }

    @Override
    public InboundDataSource dataSource() {
        return (InboundDataSource) super.dataSource();
    }
}
