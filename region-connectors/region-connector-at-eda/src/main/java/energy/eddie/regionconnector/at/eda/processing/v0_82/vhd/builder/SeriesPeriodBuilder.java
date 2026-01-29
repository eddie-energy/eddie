// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import energy.eddie.cim.v0_82.vhd.ESMPDateTimeIntervalComplexType;
import energy.eddie.cim.v0_82.vhd.PointComplexType;
import energy.eddie.cim.v0_82.vhd.QualityTypeList;
import energy.eddie.cim.v0_82.vhd.SeriesPeriodComplexType;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.Energy;
import energy.eddie.regionconnector.at.eda.dto.EnergyData;
import energy.eddie.regionconnector.at.eda.dto.EnergyPosition;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SeriesPeriodBuilder {

    private final SeriesPeriodComplexType seriesPeriod = new SeriesPeriodComplexType()
            .withReasonList(new SeriesPeriodComplexType.ReasonList());

    public SeriesPeriodBuilder withEnergyData(EnergyData energyData) throws InvalidMappingException {
        List<PointComplexType> points = new ArrayList<>();
        var energyPositions = energyData.energyPositions();
        for (int i = 0; i < energyPositions.size(); i++) {
            EnergyPosition meterReading = energyPositions.get(i);

            PointComplexType point = new PointComplexType();
            point.setPosition(String.valueOf(i));
            point.setEnergyQuantityQuantity(meterReading.billingQuantity());
            point.setEnergyQuantityQuality(switch (meterReading.meteringMethod()) {
                case "L1" -> QualityTypeList.AS_PROVIDED;
                case "L2" -> QualityTypeList.ADJUSTED;
                case "L3" -> QualityTypeList.ESTIMATED;
                default ->
                        throw new InvalidMappingException("Unexpected MeteringMethod value + '" + meterReading.meteringMethod() + "' in consumptionRecord");
            });
            points.add(point);
        }

        seriesPeriod.withPointList(new SeriesPeriodComplexType.PointList().withPoints(points));

        return this;
    }

    public SeriesPeriodComplexType build() {
        return seriesPeriod;
    }

    public SeriesPeriodBuilder withEnergy(Energy energy) {
        var granularity = energy.granularity();
        var duration = granularity == null ? Duration.ZERO.toString() : granularity.toString();
        seriesPeriod.setResolution(duration);

        var interval = new EsmpTimeInterval(
                energy.meterReadingStart(),
                energy.meterReadingEnd()
        );

        seriesPeriod.withTimeInterval(new ESMPDateTimeIntervalComplexType()
                                              .withStart(interval.start())
                                              .withEnd(interval.end())
        );

        return this;
    }
}
