// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
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
            ORDER BY permission_id, creation_date DESC, id DESC
        ) AS latest_status_message ORDER BY creation_date DESC, id DESC;
    """, nativeQuery = true)
    List<StatusMessage> findLatestStatusMessageForAllPermissions();

    @Query(value = """
        WITH latest_status_message AS (
            SELECT DISTINCT ON (permission_id) *
            FROM admin_console.status_messages
            ORDER BY permission_id, creation_date DESC, id DESC
        )
        SELECT * FROM latest_status_message
        ORDER BY creation_date DESC, id DESC;
    """, countQuery = """
        SELECT count(*) FROM (
              SELECT DISTINCT ON (permission_id) *
              FROM admin_console.status_messages
              ORDER BY permission_id, creation_date DESC, id DESC
        );
    """, nativeQuery = true)
    Page<StatusMessage> findLatestStatusMessageForPaginatedPermissions(Pageable pageable);

    List<StatusMessage> findByPermissionIdOrderByCreationDateDescIdDesc(String permissionId);
}
