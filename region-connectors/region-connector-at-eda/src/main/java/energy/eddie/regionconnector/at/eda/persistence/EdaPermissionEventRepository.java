// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.at.eda.permission.request.events.PersistablePermissionEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EdaPermissionEventRepository extends JpaRepository<PersistablePermissionEvent, Long>, PermissionEventRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PersistablePermissionEvent> findFirstByPermissionIdInOrderByIdAsc(List<String> permissionIds);

    @Query("""
            SELECT event.permissionId FROM AcceptedEvent event
            WHERE event.permissionId IN :permissionIds
            GROUP BY event.permissionId
            HAVING COUNT(DISTINCT event.meteringPointId) > 1 OR COUNT(DISTINCT event.cmConsentId) > 1
            """)
    List<String> findInconsistentPermissionIds(@Param("permissionIds") List<String> permissionIds);
}
