// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.master.data;

public record PermissionAdministrator(
        String country,
        String company,
        String name,
        String companyId,
        String jumpOffUrl,
        String regionConnector
) {
}
