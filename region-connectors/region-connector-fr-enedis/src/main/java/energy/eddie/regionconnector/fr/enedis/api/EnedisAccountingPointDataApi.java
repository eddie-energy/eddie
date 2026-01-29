// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.regionconnector.fr.enedis.dto.address.CustomerAddress;
import energy.eddie.regionconnector.fr.enedis.dto.contact.CustomerContact;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.identity.CustomerIdentity;
import reactor.core.publisher.Mono;

public interface EnedisAccountingPointDataApi {

    /**
     * Retrieves the contract data for a specified usage point.
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @return A {@link Mono} that emits the {@link CustomerContract} data for the specified usage point or an error
     */
    Mono<CustomerContract> getContract(String usagePointId);

    /**
     * Retrieves the address data for a specified usage point.
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @return A {@link Mono} that emits the {@link CustomerAddress} data for the specified usage point or an error
     */
    Mono<CustomerAddress> getAddress(String usagePointId);

    /**
     * Retrieves the identity data for a specified usage point.
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @return A {@link Mono} that emits the {@link CustomerIdentity} data for the specified usage point or an error
     */
    Mono<CustomerIdentity> getIdentity(String usagePointId);

    /**
     * Retrieves the contact data for a specified usage point.
     *
     * @param usagePointId The unique identifier for the usage point. Must not be null or empty.
     * @return A {@link Mono} that emits the {@link CustomerContact} data for the specified usage point or an error
     */
    Mono<CustomerContact> getContact(String usagePointId);
}
