// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.masterdata;

import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public interface ContractPartner {
    @Nullable
    String salutation();

    @Nullable
    String surname();

    @Nullable
    String firstName();

    @Nullable
    String companyName();

    @Nullable
    String contractPartnerNumber();

    @Nullable
    ZonedDateTime dateOfBirth();

    @Nullable
    ZonedDateTime dateOfDeath();

    @Nullable
    String companyRegisterNumber();

    @Nullable
    String vatNumber();

    @Nullable
    String email();
}
