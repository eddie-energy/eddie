// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import java.util.List;

public class SimpleEnergyData implements EnergyData {
    private List<EnergyPosition> energyPositions;
    private String meterCode;
    private String billingUnit;

    @Override
    public List<EnergyPosition> energyPositions() {
        return energyPositions;
    }

    @Override
    public String meterCode() {
        return meterCode;
    }

    @Override
    public String billingUnit() {
        return billingUnit;
    }

    public SimpleEnergyData setEnergyPositions(List<EnergyPosition> energyPositions) {
        this.energyPositions = energyPositions;
        return this;
    }

    public SimpleEnergyData setMeterCode(String meterCode) {
        this.meterCode = meterCode;
        return this;
    }

    public SimpleEnergyData setBillingUnit(String billingUnit) {
        this.billingUnit = billingUnit;
        return this;
    }
}
