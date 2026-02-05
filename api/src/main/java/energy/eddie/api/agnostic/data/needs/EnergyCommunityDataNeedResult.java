// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

import java.time.LocalDate;

public record EnergyCommunityDataNeedResult(LocalDate start) implements DataNeedCalculationResult {
}
