// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.masterdata;

import jakarta.annotation.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;

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
    XMLGregorianCalendar dateOfBirth();

    @Nullable
    XMLGregorianCalendar dateOfDeath();

    @Nullable
    String companyRegisterNumber();

    @Nullable
    String vatNumber();

    @Nullable
    String email();
}
