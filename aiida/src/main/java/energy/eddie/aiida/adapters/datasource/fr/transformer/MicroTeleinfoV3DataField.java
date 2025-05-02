package energy.eddie.aiida.adapters.datasource.fr.transformer;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Optional;

public record MicroTeleinfoV3DataField(@JsonProperty String raw,
                                       @JsonProperty Object value,
                                       UnitOfMeasurement unitOfMeasurement,
                                       ObisCode mappedObisCode,
                                       @JsonProperty Optional<MicroTeleinfoV3Timestamp> timestamp
) {}
