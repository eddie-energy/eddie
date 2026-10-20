// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.regionconnector.fr.enedis.dto.address.UsagePointGeneralData;
import energy.eddie.regionconnector.fr.enedis.dto.situation.ContractualSituation;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EnedisAccountingPointDataApi {

    /**
     * Retrieves the contractual situations of a usage point, including the contracts, the customer
     * identity and the customer contact data. The API returns an array, so a usage point can carry
     * more than one situation (for instance consumption and production).
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @return A {@link Mono} that emits the {@link ContractualSituation} list for the specified usage point or an error
     */
    Mono<List<ContractualSituation>> getContract(String usagePointId);

    /**
     * Retrieves the general data (installation address) of a usage point.
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @return A {@link Mono} that emits the {@link UsagePointGeneralData} for the specified usage point or an error
     */
    Mono<UsagePointGeneralData> getAddress(String usagePointId);
}