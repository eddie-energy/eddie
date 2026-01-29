// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.master.data;

public record MeteredDataAdministrator(
        String country,
        String company,
        String companyId,
        String websiteUrl,
        String officialContact,
        String permissionAdministrator
) {
}
