// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.dtos;

import java.util.Set;

/**
 * DTO used to represent the supported {@code dataNeeds} for a specific {@code regionConnectorId}
 */
public record SupportedDataNeeds(String regionConnectorId, Set<String> dataNeeds) {}