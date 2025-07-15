package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;

/**
 * The simulation context contains information, which is not available during the {@link energy.eddie.regionconnector.simulation.engine.steps.Model} creation, but during the runtime phase.
 * Can be thought of similar to command line arguments.
 *
 * @param documentStreams the streams that a {@link energy.eddie.regionconnector.simulation.engine.steps.Step} can emit to
 * @param cimConfig       information about the configuration of the EDDIE instance
 * @param permissionId    provided by the entity executing the test simulation
 * @param connectionId    provided by the entity executing the test simulation
 * @param dataNeedId      provided by the entity executing the test simulation, must be a valid data need ID
 */
public record SimulationContext(DocumentStreams documentStreams,
                                CommonInformationModelConfiguration cimConfig,
                                String permissionId,
                                String connectionId,
                                String dataNeedId) {
}
