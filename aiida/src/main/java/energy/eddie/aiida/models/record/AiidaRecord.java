// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaRecordDto;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class AiidaRecord {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Used just as JPA ID
    @SuppressWarnings({"unused", "NullAway"})
    private Long id;
    @JsonProperty
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant timestamp;
    @JsonProperty
    @Enumerated(EnumType.STRING)
    private AiidaAsset asset;
    @JsonProperty
    private UUID userId;
    @JsonProperty
    protected UUID dataSourceId;
    @OneToMany(mappedBy = "aiidaRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("values")
    private List<AiidaRecordValue> aiidaRecordValues;

    public AiidaRecord(
            Instant timestamp,
            AiidaAsset asset,
            UUID userId,
            UUID dataSourceId,
            List<AiidaRecordValue> aiidaRecordValues
    ) {
        this.timestamp = timestamp;
        this.asset = asset;
        this.userId = userId;
        this.dataSourceId = dataSourceId;
        this.aiidaRecordValues = aiidaRecordValues;
    }

    /**
     * Constructor only for JPA.
     */
    @SuppressWarnings("NullAway.Init")
    protected AiidaRecord() {
    }

    public AiidaRecord(AiidaRecord aiidaRecord) {
        this.id = aiidaRecord.id;
        this.timestamp = aiidaRecord.timestamp;
        this.asset = aiidaRecord.asset;
        this.userId = aiidaRecord.userId;
        this.dataSourceId = aiidaRecord.dataSourceId;
        this.aiidaRecordValues = aiidaRecord.aiidaRecordValues;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public List<AiidaRecordValue> aiidaRecordValues() {
        return aiidaRecordValues;
    }

    public void setAiidaRecordValues(List<AiidaRecordValue> aiidaRecordValues) {
        this.aiidaRecordValues = aiidaRecordValues;
    }

    public AiidaAsset asset() {
        return asset;
    }

    public Long id() {
        return id;
    }

    public UUID userId() {return userId;}

    public UUID dataSourceId() {
        return dataSourceId;
    }

    public AiidaRecordDto toDto(UUID permissionId) {
        return new AiidaRecordDto(asset,
                                  userId,
                                  dataSourceId,
                                  permissionId,
                                  aiidaRecordValues.stream()
                                                   .map(AiidaRecordValue::toDto)
                                                   .toList());
    }
}
