// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.permission;

import jakarta.annotation.Nullable;

import java.util.UUID;

/**
 * Accepts a permission request from an AIIDA code. Permissions are only persisted once accepted, therefore the
 * handshake information of the AIIDA code has to be passed again.
 *
 * @see AcceptOutboundPermissionDto
 * @see AcceptInboundPermissionDto
 */
public sealed interface AcceptPermissionDto permits AcceptOutboundPermissionDto, AcceptInboundPermissionDto {
    /**
     * ID of the EDDIE framework that created the permission request, from the AIIDA code.
     */
    UUID eddieId();

    /**
     * Handshake URL template from the AIIDA code.
     */
    String handshakeUrl();

    /**
     * Access token from the AIIDA code.
     */
    String accessToken();

    /**
     * Optional display name, defaults to the name of the data need.
     */
    @Nullable
    String displayName();
}
