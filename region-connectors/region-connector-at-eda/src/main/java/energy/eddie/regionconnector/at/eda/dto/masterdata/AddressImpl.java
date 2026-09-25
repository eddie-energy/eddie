// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.masterdata;

import jakarta.annotation.Nullable;

public record AddressImpl(
        @Nullable String zipCode,
        @Nullable String city,
        @Nullable String street,
        @Nullable String streetNumber,
        @Nullable String staircase,
        @Nullable String floor,
        @Nullable String door
) implements Address {
}
