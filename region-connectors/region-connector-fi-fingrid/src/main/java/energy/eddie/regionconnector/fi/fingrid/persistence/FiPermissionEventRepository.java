// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.fi.fingrid.permission.events.PersistablePermissionEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface FiPermissionEventRepository extends PermissionEventRepository, org.springframework.data.repository.Repository<PersistablePermissionEvent, Long> {
}
