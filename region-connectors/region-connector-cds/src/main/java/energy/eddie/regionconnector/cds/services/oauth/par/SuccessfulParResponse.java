// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth.par;

import java.net.URI;
import java.time.ZonedDateTime;

public record SuccessfulParResponse(URI redirectUri, ZonedDateTime expiresAt, String state) implements ParResponse {
}
