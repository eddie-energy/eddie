package energy.eddie.aiida.adapters.datasource.fr.mode;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Optional;

public record MicroTeleinfoV3DataField(String raw,
                                       Object value,
                                       UnitOfMeasurement unitOfMeasurement,
                                       ObisCode mappedObisCode,
                                       Optional<MicroTeleinfoV3Timestamp> timestamp
) {}
