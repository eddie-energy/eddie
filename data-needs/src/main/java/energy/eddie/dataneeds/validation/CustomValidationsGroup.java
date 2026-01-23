// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.validation;

import jakarta.validation.constraints.NotNull;

/**
 * For validations that depend on other validations to be executed beforehand (e.g. {@link NotNull}). See JavaDoc of
 * {@link BasicValidationsGroup} for more information.
 */
public interface CustomValidationsGroup {
}
