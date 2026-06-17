// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation;

import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

class SimulationConnectorTest {

    @Test
    void testTerminatePermission() {
        // Given
        var cimConfig = new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME,
                                                                     "ep-id");
        var streams = new DocumentStreams(cimConfig, new ObjectMapper());
        var simulationConnector = new SimulationConnector(streams);
        var testPermissionId = "test-permission-id";

        // When
        simulationConnector.terminatePermission(testPermissionId);

        // Then
        StepVerifier.create(streams.getTerminationStream())
                    .expectNext(testPermissionId)
                    .then(streams::close)
                    .verifyComplete();
    }
}