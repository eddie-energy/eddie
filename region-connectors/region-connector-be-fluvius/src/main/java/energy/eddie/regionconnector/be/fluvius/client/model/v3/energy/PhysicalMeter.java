// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record PhysicalMeter(@Nullable String seqNumber,
                            @Nullable String meterId,
                            @Nullable List<MeasurementSlice> dailyEnergy,
                            @Nullable List<MeasurementSlice> hourlyEnergy,
                            @Nullable List<MeasurementSlice> quarterHourlyEnergy) implements EnergyForGranularity {
}
