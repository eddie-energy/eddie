// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import energy.eddie.api.agnostic.Granularity;

import java.time.ZonedDateTime;
import java.util.List;

public class SimpleEnergy implements Energy {


    private Granularity granularity;
    private List<EnergyData> energyData;
    private ZonedDateTime meterReadingStart;
    private ZonedDateTime meterReadingEnd;
    private String meteringReason;

    @Override
    public Granularity granularity() {
        return granularity;
    }

    @Override
    public List<EnergyData> energyData() {
        return energyData;
    }

    @Override
    public ZonedDateTime meterReadingStart() {
        return meterReadingStart;
    }

    @Override
    public ZonedDateTime meterReadingEnd() {
        return meterReadingEnd;
    }

    @Override
    public String meteringReason() {
        return meteringReason;
    }

    public SimpleEnergy setGranularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public SimpleEnergy setEnergyData(List<EnergyData> energyData) {
        this.energyData = energyData;
        return this;
    }

    public SimpleEnergy setMeterReadingStart(ZonedDateTime meterReadingStart) {
        this.meterReadingStart = meterReadingStart;
        return this;
    }

    public SimpleEnergy setMeterReadingEnd(ZonedDateTime meterReadingEnd) {
        this.meterReadingEnd = meterReadingEnd;
        return this;
    }

    public SimpleEnergy setMeteringReason(String meteringReason) {
        this.meteringReason = meteringReason;
        return this;
    }
}
