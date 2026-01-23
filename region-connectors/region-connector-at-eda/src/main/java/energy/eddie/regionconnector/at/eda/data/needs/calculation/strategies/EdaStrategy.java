// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.EnergyDataTimeframeStrategy;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public class EdaStrategy implements EnergyDataTimeframeStrategy {
    /**
     * Calculates the start and end date of the energy data, if the data need requires energy data.
     * Otherwise, it returns null.
     *
     * @param dataNeed          the data need
     * @param referenceDateTime the reference datetime that is used to calculate the timeframe.
     * @return start and end date of the energy data.
     * @throws UnsupportedDataNeedException if the data need is not meant for energy data
     */
    @Override
    @Nullable
    public Timeframe energyDataTimeframe(
            DataNeed dataNeed,
            ZonedDateTime referenceDateTime
    ) throws UnsupportedDataNeedException {
        if (!(dataNeed instanceof TimeframedDataNeed timeframedDataNeed)) {
            return null;
        }
        var now = referenceDateTime.toLocalDate();
        DataNeedWrapper wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(
                timeframedDataNeed,
                now,
                EdaRegionConnectorMetadata.PERIOD_EARLIEST_START,
                EdaRegionConnectorMetadata.PERIOD_LATEST_END
        );
        return new Timeframe(wrapper.calculatedStart(), wrapper.calculatedEnd());
    }
}
