// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.admin.console.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusMessageRepository extends JpaRepository<StatusMessage, Long> {
    @Query(value = """
        SELECT * FROM (
            SELECT DISTINCT ON (permission_id) *
            FROM admin_console.status_messages
            ORDER BY permission_id, start_date DESC
        ) AS latest_status_message ORDER BY start_date DESC;
    """, nativeQuery = true)
    List<StatusMessage> findLatestStatusMessageForAllPermissions();

    @Query(value = """
        WITH latest_status_message AS (
            SELECT DISTINCT ON (permission_id) *
            FROM admin_console.status_messages
            ORDER BY permission_id, start_date DESC
        )
        SELECT * FROM latest_status_message
        ORDER BY start_date DESC;
    """, countQuery = """
        SELECT count(*) FROM (
              SELECT DISTINCT ON (permission_id) *
              FROM admin_console.status_messages
              ORDER BY permission_id, start_date DESC
        );
    """, nativeQuery = true)
    Page<StatusMessage> findLatestStatusMessageForPaginatedPermissions(Pageable pageable);

    List<StatusMessage> findByPermissionIdOrderByStartDateDescIdDesc(String permissionId);
}
