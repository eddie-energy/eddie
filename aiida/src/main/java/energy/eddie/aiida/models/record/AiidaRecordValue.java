// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.aiida.AiidaRecordValueDto;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

@Entity
public class AiidaRecordValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aiida_record_id", referencedColumnName = "id")
    @JsonIgnore
    private AiidaRecord aiidaRecord;

    @Column(name = "raw_tag", nullable = false)
    @Schema(description = "The rawTag tag associated to a rawTag value.")
    @JsonProperty
    private String rawTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_tag", nullable = false)
    @Schema(description = "The data tag (an OBIS code) associated to a value.")
    @JsonProperty
    private ObisCode dataTag;

    @Column(name = "raw_value", nullable = false)
    @Schema(description = "The rawTag value of the rawTag tag.")
    @JsonProperty
    private String rawValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "raw_unit_of_measurement", nullable = false)
    @Schema(description = "The unit of measurement of the rawTag value related to the rawTag tag.")
    @JsonProperty
    private UnitOfMeasurement rawUnitOfMeasurement;

    @Column(nullable = false)
    @Schema(description = "The value of the data tag.")
    @JsonProperty
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_of_measurement", nullable = false)
    @Schema(description = "The unit of measurement of the value related to the data tag.")
    @JsonProperty
    private UnitOfMeasurement unitOfMeasurement;

    @SuppressWarnings("NullAway.Init")
    public AiidaRecordValue(
            String rawTag,
            ObisCode dataTag,
            String rawValue,
            UnitOfMeasurement rawUnitOfMeasurement,
            String value,
            UnitOfMeasurement unitOfMeasurement
    ) {
        this.rawTag = rawTag;
        this.dataTag = dataTag;
        this.rawValue = rawValue;
        this.rawUnitOfMeasurement = rawUnitOfMeasurement;
        this.value = value;
        this.unitOfMeasurement = unitOfMeasurement;
    }

    @SuppressWarnings("NullAway.Init")
    public AiidaRecordValue() {
    }

    public Long id() {
        return id;
    }

    public AiidaRecord aiidaRecord() {
        return aiidaRecord;
    }

    public void setAiidaRecord(AiidaRecord aiidaRecord) {
        this.aiidaRecord = aiidaRecord;
    }

    public ObisCode dataTag() {
        return dataTag;
    }

    public String rawTag() {
        return rawTag;
    }

    public String value() {
        return value;
    }

    public String rawValue() {
        return rawValue;
    }

    public UnitOfMeasurement unitOfMeasurement() {
        return unitOfMeasurement;
    }

    public UnitOfMeasurement rawUnitOfMeasurement() {
        return rawUnitOfMeasurement;
    }

    public AiidaRecordValueDto toDto() {
        return new AiidaRecordValueDto(
                rawTag,
                dataTag,
                rawValue,
                value,
                rawUnitOfMeasurement,
                unitOfMeasurement
        );
    }
}
