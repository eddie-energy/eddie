// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client;

import java.net.URI;

public record CustomerDataClientCredentials(String clientId, String clientSecret, URI tokenEndpoint) {
}
