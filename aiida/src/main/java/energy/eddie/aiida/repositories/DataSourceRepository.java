// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.datasource.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DataSourceRepository extends JpaRepository<DataSource, UUID> {

    @Query("select d from DataSource d where d.userId = :userId and not d.type = DataSourceType.INBOUND")
    List<DataSource> findOutboundByUserId(UUID userId);
}