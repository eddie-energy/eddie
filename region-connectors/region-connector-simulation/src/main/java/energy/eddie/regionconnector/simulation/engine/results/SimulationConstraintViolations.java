// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.results;

import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;

import java.util.List;

public record SimulationConstraintViolations(List<ConstraintViolation> violations) implements SimulationResult {
}
