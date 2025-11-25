package energy.eddie.aiida.adapters.datasource;

import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

public interface AdapterMeasurement {
    String entryKey();

    UnitOfMeasurement rawUnitOfMeasurement();

    String rawValue();
}
