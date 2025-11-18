package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

public record SmartGatewaysAdapterMessageField(String rawTag, String value, UnitOfMeasurement unitOfMeasurement, ObisCode obisCode) {}
