// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.ee.elering.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.ee.elering.permission.events.PersistablePermissionEvent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EePermissionEventRepository extends PermissionEventRepository, CrudRepository<PersistablePermissionEvent, Long> {
}
