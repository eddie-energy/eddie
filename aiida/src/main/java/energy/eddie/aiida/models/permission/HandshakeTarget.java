// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

/**
 * Identifies the handshake endpoint of a permission request at the EDDIE framework, as contained in the AIIDA code.
 *
 * @param handshakeUrl URL of the permission request at the EDDIE framework.
 * @param accessToken  Access token authorizing requests to the handshake URL.
 */
public record HandshakeTarget(String handshakeUrl, String accessToken) {
    public static HandshakeTarget of(Permission permission) {
        return new HandshakeTarget(permission.handshakeUrl(), permission.accessToken());
    }
}
