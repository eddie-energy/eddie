// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
public class InboundRecord {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Used just as JPA ID
    @SuppressWarnings({"unused", "NullAway"})
    private Long id;
    @JsonProperty
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    protected Instant timestamp;
    @JsonProperty
    @Enumerated(EnumType.STRING)
    protected AiidaAsset asset;
    @JsonProperty
    protected UUID userId;
    @JsonProperty
    protected UUID dataSourceId;
    @JsonProperty
    private String payload;

    public InboundRecord(
            Instant timestamp,
            AiidaAsset asset,
            UUID userId,
            UUID dataSourceId,
            String payload
    ) {
        this.timestamp = timestamp;
        this.asset = asset;
        this.userId = userId;
        this.dataSourceId = dataSourceId;
        this.payload = payload;
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected InboundRecord() {
    }

    public Long id() {
        return id;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public AiidaAsset asset() {
        return asset;
    }

    public UUID userId() {return userId;}

    public UUID dataSourceId() {
        return dataSourceId;
    }

    public String payload() {
        return payload;
    }
}
