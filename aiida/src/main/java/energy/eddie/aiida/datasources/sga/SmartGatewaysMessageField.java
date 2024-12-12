package energy.eddie.aiida.datasources.sga;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public record SmartGatewaysMessageField(String rawTag, String value, UnitOfMeasurement unitOfMeasurement, ObisCode obisCode) {}
