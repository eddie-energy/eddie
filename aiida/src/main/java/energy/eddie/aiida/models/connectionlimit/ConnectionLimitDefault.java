// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.connectionlimit;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record ConnectionLimitDefault(UUID permissionId, @Nullable String meterId, BigDecimal minLimitKw, BigDecimal maxLimitKw) {}
