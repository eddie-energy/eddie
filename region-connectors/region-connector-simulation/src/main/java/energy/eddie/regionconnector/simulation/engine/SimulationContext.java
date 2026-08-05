// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationResult;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;

import java.time.ZonedDateTime;

/**
 * The simulation context contains information, which is not available during the {@link energy.eddie.regionconnector.simulation.engine.steps.Model} creation, but during the runtime phase.
 * Can be thought of similar to command line arguments.
 *
 * @param documentStreams the streams that a {@link energy.eddie.regionconnector.simulation.engine.steps.Step} can emit to
 * @param permissionId    provided by the entity executing the test simulation
 * @param connectionId    provided by the entity executing the test simulation
 * @param dataNeedId      provided by the entity executing the test simulation, must be a valid data need ID
 */
public record SimulationContext(DocumentStreams documentStreams,
                                String permissionId,
                                String connectionId,
                                String dataNeedId,
                                ZonedDateTime creationDateTime,
                                DataNeedCalculationResult calculationResult) {


    public DataNeed dataNeed() {
        return switch (calculationResult) {
            case DataNeedCalculationResult.DataNeedCalculationSuccessResult<?> r -> r.dataNeed();
            default -> throw new IllegalStateException(
                    "Data need calculation result is not of type DataNeedCalculationSuccessResult");
        };
    }
}
