// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource;

import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;

public interface AdapterMeasurement {
    String entryKey();

    UnitOfMeasurement rawUnitOfMeasurement();

    String rawValue();
}
