// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.validation;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotNull;

/**
 * Intended to be used in combination with {@link CustomValidationsGroup} in {@link GroupSequence} annotation and be
 * applied to basic constraints like {@link NotNull}. This should be the first group, to ensure the basic constraints
 * are validated before the custom constraints are validated. If this is not used, the custom constraints may be
 * validated before the basic ones and one would have to add null checks in the custom constraints.
 */
public interface BasicValidationsGroup {
}
