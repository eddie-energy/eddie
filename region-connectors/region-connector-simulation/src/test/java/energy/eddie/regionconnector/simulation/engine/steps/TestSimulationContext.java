// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public class TestSimulationContext {
    public static SimulationContext create() {
        var now = LocalDate.now(ZoneOffset.UTC);
        return new SimulationContext(
                new DocumentStreams(new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.FINLAND_NATIONAL_CODING_SCHEME,
                                                                                 "EP-ID"), new ObjectMapper()),
                "pid",
                "cid",
                "dnid",
                ZonedDateTime.now(ZoneOffset.UTC),
                new ValidatedHistoricalDataDataNeedResult(
                        List.of(),
                        null,
                        new Timeframe(now, now),
                        new ValidatedHistoricalDataDataNeed()
                )
        );
    }
}
