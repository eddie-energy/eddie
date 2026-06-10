// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import org.jspecify.annotations.Nullable;

public record Total(@Nullable Double value,
                    Unit unit,
                    @Nullable ValidationState validationState,
                    @Nullable GasConversionFactor gasConversionFactor) {}
