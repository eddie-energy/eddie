// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimitMonitoring;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConnectionLimitMonitoringRepository extends JpaRepository<ConnectionLimitMonitoring, UUID> {
    List<ConnectionLimitMonitoring> findByDataSourceId(UUID dataSourceId);
}
