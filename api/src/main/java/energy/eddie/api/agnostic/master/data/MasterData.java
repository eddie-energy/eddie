// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.master.data;

import java.util.List;
import java.util.Optional;

public interface MasterData {
    List<PermissionAdministrator> permissionAdministrators();

    Optional<PermissionAdministrator> getPermissionAdministrator(String id);

    List<MeteredDataAdministrator> meteredDataAdministrators();

    Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id);
}
