// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.api.agnostic.aiida.AiidaRecordDto;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class AiidaRecord extends DataSourceRecord {

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
        super(aiidaRecord.id, aiidaRecord.timestamp, aiidaRecord.dataSource);
        this.aiidaRecordValues = aiidaRecord.aiidaRecordValues;
    }

    public List<AiidaRecordValue> aiidaRecordValues() {
        return aiidaRecordValues;
    }

    public void setAiidaRecordValues(List<AiidaRecordValue> aiidaRecordValues) {
        this.aiidaRecordValues = aiidaRecordValues;
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
