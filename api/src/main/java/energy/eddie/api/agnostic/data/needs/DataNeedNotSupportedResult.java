// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.data.needs;

public record DataNeedNotSupportedResult(String message) implements DataNeedCalculationResult {
}
