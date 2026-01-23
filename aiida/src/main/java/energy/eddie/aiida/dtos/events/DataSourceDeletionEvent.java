// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.events;

import java.util.Set;
import java.util.UUID;

public record DataSourceDeletionEvent(Set<UUID> permissionIds) implements AiidaEvent {
}
