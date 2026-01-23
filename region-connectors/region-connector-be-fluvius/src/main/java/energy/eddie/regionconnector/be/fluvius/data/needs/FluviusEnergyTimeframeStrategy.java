// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.EnergyDataTimeframeStrategy;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public class FluviusEnergyTimeframeStrategy implements EnergyDataTimeframeStrategy {
    private final RegionConnectorMetadata regionConnectorMetadata;

    public FluviusEnergyTimeframeStrategy(RegionConnectorMetadata regionConnectorMetadata) {this.regionConnectorMetadata = regionConnectorMetadata;}

    /**
     * For Fluvius one extra day has to be requested for energy data, otherwise the last 15 or 30 minutes will be missing for a granularity of PT15M or PT30M.
     * For daily data, the permission request is fulfilled if the meter reading end data is the same data as the end - 1.
     *
     * @param dataNeed          The data need is the basis for the timeframe calculation
     * @param referenceDateTime The reference datetime has to be used to calculate the start and end date. Usually the created datetime of the {@link energy.eddie.api.agnostic.process.model.PermissionRequest}.
     * @return the energy timeframe
     * @throws UnsupportedDataNeedException if the data need is not supported
     */
    @Nullable
    @Override
    public Timeframe energyDataTimeframe(
            DataNeed dataNeed,
            ZonedDateTime referenceDateTime
    ) throws UnsupportedDataNeedException {
        var now = referenceDateTime.toLocalDate();
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
            return new Timeframe(now, now.plusDays(1));
        }
        var wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(
                vhdDataNeed,
                now,
                regionConnectorMetadata.earliestStart(),
                regionConnectorMetadata.latestEnd()
        );
        return new Timeframe(wrapper.calculatedStart(), wrapper.calculatedEnd().plusDays(1));
    }
}
