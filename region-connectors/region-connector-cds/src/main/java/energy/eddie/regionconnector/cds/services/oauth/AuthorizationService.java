// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.master.data.CdsServer;

import java.net.URI;

public interface AuthorizationService {
    URI createOAuthRequest(CdsServer cdsServer, String permissionId);
}
