// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Response of the subscribed services API ({@code POST /subscribed_services/v1}).
 *
 * @param totalServices Total number of subscribed services matching the search criteria.
 * @param services      The subscribed services.
 */
public record ServiceSubscriptionsResponse(
        @JsonProperty("nbTotalServices") @Nullable Long totalServices,
        @JsonProperty("serviceSouscrit") @Nullable List<SubscribedService> services
) {
}
