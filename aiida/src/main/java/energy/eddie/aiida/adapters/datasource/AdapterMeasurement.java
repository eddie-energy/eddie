package energy.eddie.aiida.adapters.datasource;

import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

public interface AdapterMeasurement {
    String entryKey();

    ObisCode obisCode();

    UnitOfMeasurement rawUnitOfMeasurement();

    UnitOfMeasurement unitOfMeasurement();

    String rawValue();

    String value();
}
