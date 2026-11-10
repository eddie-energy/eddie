// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.events;

import energy.eddie.api.agnostic.aiida.AiidaAsset;

import java.util.UUID;

/**
 * Published after an inbound permission was accepted to create its inbound data source.
 *
 * @param permissionId Id of the accepted inbound permission.
 * @param asset        Asset of the inbound data source, one of the assets allowed by the data need.
 */
public record InboundPermissionAcceptEvent(UUID permissionId, AiidaAsset asset) implements AiidaEvent {
}
