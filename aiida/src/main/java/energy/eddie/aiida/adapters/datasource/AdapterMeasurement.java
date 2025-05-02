package energy.eddie.aiida.adapters.datasource;

import energy.eddie.aiida.models.record.UnitOfMeasurement;

public interface AdapterMeasurement {
    String entryKey();

    UnitOfMeasurement rawUnitOfMeasurement();

    String rawValue();
}
