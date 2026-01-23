// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiidaLocalDataNeedRepository extends JpaRepository<AiidaLocalDataNeed, UUID> {
}
