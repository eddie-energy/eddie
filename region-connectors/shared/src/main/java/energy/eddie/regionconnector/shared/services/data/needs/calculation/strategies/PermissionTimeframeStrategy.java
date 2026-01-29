// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

@FunctionalInterface
public interface PermissionTimeframeStrategy {
    Timeframe permissionTimeframe(@Nullable Timeframe energyDataTimeframe, ZonedDateTime referenceDateTime);
}
