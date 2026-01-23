// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;

public class TestSimulationContext {
    public static SimulationContext create() {
        return new SimulationContext(
                new DocumentStreams(new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME, "EP-ID")),
                "pid",
                "cid",
                "dnid"
        );
    }
}
