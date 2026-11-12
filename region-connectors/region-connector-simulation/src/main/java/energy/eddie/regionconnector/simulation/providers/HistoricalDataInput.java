// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.providers;

import energy.eddie.regionconnector.shared.cim.HistoricalDataEmissionMetadata;

import energy.eddie.regionconnector.simulation.dtos.SimulatedMeterReading;

public record HistoricalDataInput(
        SimulatedMeterReading source,
        HistoricalDataEmissionMetadata metadata
) {
    public HistoricalDataInput(SimulatedMeterReading source) {
        this(source, new HistoricalDataEmissionMetadata());
    }
}
