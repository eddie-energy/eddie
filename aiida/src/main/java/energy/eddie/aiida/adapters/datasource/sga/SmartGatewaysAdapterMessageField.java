package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public record SmartGatewaysAdapterMessageField(String rawTag, String value, UnitOfMeasurement unitOfMeasurement, ObisCode obisCode) {}
