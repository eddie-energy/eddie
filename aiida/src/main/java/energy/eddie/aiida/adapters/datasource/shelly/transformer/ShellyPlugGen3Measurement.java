// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import energy.eddie.aiida.adapters.datasource.SmartMeterAdapterMeasurement;
import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

public class ShellyPlugGen3Measurement extends SmartMeterAdapterMeasurement {
    private final ObisCode obisCode;
    private final UnitOfMeasurement unitOfMeasurement;

    public ShellyPlugGen3Measurement(String entryKey, String rawValue, ObisCode obisCode, UnitOfMeasurement unitOfMeasurement) {
        super(entryKey, rawValue);
        this.obisCode = obisCode;
        this.unitOfMeasurement = unitOfMeasurement;
    }

    @Override
    public ObisCode obisCode() {
        return obisCode;
    }

    @Override
    public UnitOfMeasurement rawUnitOfMeasurement() {
        return unitOfMeasurement;
    }
}
