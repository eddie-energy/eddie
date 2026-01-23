// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlPermissionEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface MijnAansluitingPermissionEventRepository extends PermissionEventRepository, org.springframework.data.repository.Repository<NlPermissionEvent, Long> {

}
