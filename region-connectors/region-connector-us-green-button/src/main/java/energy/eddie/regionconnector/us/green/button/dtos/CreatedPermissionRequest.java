// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.dtos;

import java.net.URI;

public record CreatedPermissionRequest(String permissionId, URI redirectUri) {
}
