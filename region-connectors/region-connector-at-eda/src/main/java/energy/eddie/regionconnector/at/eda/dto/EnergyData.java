// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import java.util.List;

public interface EnergyData {
    List<EnergyPosition> energyPositions();

    String meterCode();

    String billingUnit();
}
