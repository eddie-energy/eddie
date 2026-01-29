// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.dtos;

import energy.eddie.regionconnector.simulation.engine.steps.Scenario;

public record ScenarioRunConfiguration(Scenario scenario, ScenarioMetadata metadata) {
}
