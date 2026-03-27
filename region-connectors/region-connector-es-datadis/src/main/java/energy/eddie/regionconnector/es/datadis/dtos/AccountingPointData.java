// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.dtos.authorizations.AuthorizedCups;

public record AccountingPointData(
        Supply supply,
        ContractDetails contractDetails,
        AuthorizedCups thirdAuthorizedUsersCups
) {
}
