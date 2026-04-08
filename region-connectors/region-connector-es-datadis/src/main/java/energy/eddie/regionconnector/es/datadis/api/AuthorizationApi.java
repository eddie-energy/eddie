// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.api;

import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequest;
import energy.eddie.regionconnector.es.datadis.dtos.AuthorizationRequestResponse;
import energy.eddie.regionconnector.es.datadis.dtos.authorizations.UserAuthorizationsResponse;
import reactor.core.publisher.Mono;

public interface AuthorizationApi {
    Mono<AuthorizationRequestResponse> postAuthorizationRequest(AuthorizationRequest authorizationRequest);

    Mono<UserAuthorizationsResponse> getThirdPartyAuthorizedUsersCups();
}
