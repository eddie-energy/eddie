// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The model is only used to define the simulation scenarios, but are never created during runtime.
 * Base class of an abstract syntax tree.
 *
 * @see <a href="https://www.jetbrains.com/help/mps/mps-project-structure.html#models">Jetbrains MPS Models</a>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        visible = true,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Scenario.class, name = Scenario.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = StatusChangeStep.class, name = StatusChangeStep.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = ValidatedHistoricalDataStep.class, name = ValidatedHistoricalDataStep.DISCRIMINATOR_VALUE)
})
public abstract class Model implements Step {
    protected final String type;

    protected Model(String type) {this.type = type;}
}
