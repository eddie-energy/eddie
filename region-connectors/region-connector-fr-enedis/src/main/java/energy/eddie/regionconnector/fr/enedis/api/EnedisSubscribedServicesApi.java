// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.regionconnector.fr.enedis.dto.subscription.ServiceSubscriptionsResponse;
import reactor.core.publisher.Mono;

public interface EnedisSubscribedServicesApi {
    /**
     * Retrieves the subscribed services for a given authorization ID.
     * This includes the usage points associated with an authorization.
     *
     * @param authorizationId the ID of the authorization
     * @return a {@link Mono} that emits the subscribed services or an error
     */
    Mono<ServiceSubscriptionsResponse> getSubscribedServices(long authorizationId);
}
