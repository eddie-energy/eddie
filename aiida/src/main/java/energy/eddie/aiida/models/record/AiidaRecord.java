// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.DataSource;
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
    @Column(nullable = false)
    @JsonProperty
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant timestamp;
    @ManyToOne
    @JoinColumn(name = "data_source_id", referencedColumnName = "id", nullable = false, updatable = false)
    @JsonIgnore
    private DataSource dataSource;
    @OneToMany(mappedBy = "aiidaRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("values")
    private List<AiidaRecordValue> aiidaRecordValues;

    public AiidaRecord(
            Instant timestamp,
            DataSource dataSource,
            List<AiidaRecordValue> aiidaRecordValues
    ) {
        this.timestamp = timestamp;
        this.dataSource = dataSource;
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
        this.dataSource = aiidaRecord.dataSource;
        this.aiidaRecordValues = aiidaRecord.aiidaRecordValues;
    }

    public Long id() {
        return id;
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

    public DataSource dataSource() {
        return dataSource;
    }

    public AiidaRecordDto toDto(UUID permissionId) {
        return new AiidaRecordDto(timestamp,
                                  permissionId,
                                  dataSource.userId(),
                                  dataSource.id(),
                                  dataSource.asset(),
                                  dataSource.meterId(),
                                  dataSource.operatorId(),
                                  aiidaRecordValues.stream()
                                                   .map(AiidaRecordValue::toDto)
                                                   .toList());
    }
}
