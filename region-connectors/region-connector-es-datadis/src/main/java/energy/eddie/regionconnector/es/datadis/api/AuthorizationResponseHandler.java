// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;

public interface AuthorizationResponseHandler {
    void handleAuthorizationRequestResponse(String permissionId, AuthorizationRequestResponse response);
}
