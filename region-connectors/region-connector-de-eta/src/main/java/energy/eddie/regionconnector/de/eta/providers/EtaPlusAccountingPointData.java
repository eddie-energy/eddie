// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;

/**
 * Deserialised payload of the accounting-point response from the ETA+ backend.
 * The backend strips null leaves and whole-null sub-objects from the wire, so
 * {@code deliveryAddress} or {@code contractParty} may arrive absent rather than
 * as an explicit JSON {@code null}; both shapes deserialise to a Java {@code null}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EtaPlusAccountingPointData(
        String meteringPointId,
        @Nullable String customerId,
        @Nullable String energyType,
        @Nullable String direction,
        @Nullable Address deliveryAddress,
        @Nullable ContractParty contractParty
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            @Nullable String streetName,
            @Nullable String buildingNumber,
            @Nullable String postalCode,
            @Nullable String city,
            @Nullable String country,
            @Nullable String addressSuffix
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContractParty(
            @Nullable String firstName,
            @Nullable String surName,
            @Nullable String companyName,
            @Nullable String email
    ) {}
}