// SPDX-FileCopyrightText: 2026 The ETA+ Developers <bilal.sakhawat@etaplus.energy>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.persistence;

import energy.eddie.regionconnector.de.eta.permission.credentials.DePermissionCredentials;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DePermissionCredentialsRepository extends CrudRepository<DePermissionCredentials, String> {

    Optional<DePermissionCredentials> findByPermissionId(String permissionId);

    void deleteByPermissionId(String permissionId);
}