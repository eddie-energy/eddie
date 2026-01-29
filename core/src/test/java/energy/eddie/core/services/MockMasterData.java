// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.master.data.MasterData;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;

import java.util.List;
import java.util.Optional;

public record MockMasterData(List<PermissionAdministrator> permissionAdministrators,
                             List<MeteredDataAdministrator> meteredDataAdministrators) implements MasterData {

    @Override
    public Optional<PermissionAdministrator> getPermissionAdministrator(String id) {
        for (var permissionAdministrator : permissionAdministrators) {
            if (permissionAdministrator.companyId().equals(id)) {
                return Optional.of(permissionAdministrator);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id) {
        for (var meteredDataAdministrator : meteredDataAdministrators) {
            if (meteredDataAdministrator.companyId().equals(id)) {
                return Optional.of(meteredDataAdministrator);
            }
        }
        return Optional.empty();
    }
}
