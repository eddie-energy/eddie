// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Transactional(readOnly = true)
public interface JpaPermissionRequestRepository extends PagingAndSortingRepository<EdaPermissionRequest, Long>, AtPermissionRequestRepository {
    @Override
    Optional<AtPermissionRequest> findByPermissionId(String permissionId);

    @Override
    @Query(value = """
            WITH original_requests AS (
                SELECT DISTINCT ON (permission_id) permission_id, conversation_id, cm_request_id
                FROM at_eda.permission_event
                WHERE dtype = 'ValidatedEvent'
                ORDER BY permission_id, id DESC
            )
            SELECT request.permission_id, request.connection_id,
                   original.cm_request_id, original.conversation_id,
                   request.created, request.data_need_id, request.dso_id, request.granularity,
                   request.metering_point_id, request.permission_start, request.permission_end,
                   request.cm_consent_id AS consent_id, request.message, request.status, request.cause,
                   request.energy_direction, request.participation_factor
            FROM at_eda.eda_permission_request request
            JOIN original_requests original USING (permission_id)
            WHERE original.conversation_id = :conversationId OR original.cm_request_id = :cmRequestId
            ORDER BY request.permission_id
            """, nativeQuery = true)
    List<AtPermissionRequestProjection> findByOriginalConversationIdOrCMRequestId(
            @Param("conversationId") String conversationId, @Param("cmRequestId") @Nullable String cmRequestId);

    @Override
    @Query(value = """
            SELECT request.*, request.cm_consent_id AS consent_id
            FROM at_eda.eda_permission_request request
            WHERE request.permission_id IN (
                SELECT permission_id FROM at_eda.permission_event
                WHERE conversation_id = :conversationId OR cm_request_id = :cmRequestId
            )
            ORDER BY request.permission_id
            """, nativeQuery = true)
    List<AtPermissionRequestProjection> findByConversationIdOrCMRequestId(
            @Param("conversationId") String conversationId, @Param("cmRequestId") @Nullable String cmRequestId);

    @Override
    @Query("SELECT pr FROM EdaPermissionRequest pr WHERE pr.conversationId = :conversationId AND pr.meteringPointId = :meteringPointId")
    Optional<AtPermissionRequest> findByConversationIdAndMeteringPointId(
            @Param("conversationId") String conversationId, @Param("meteringPointId") @Nullable String meteringPointId);

    @Override
    @Query(value = """
            SELECT request.*, request.cm_consent_id AS consent_id
            FROM at_eda.eda_permission_request request
            WHERE request.metering_point_id = :meteringPointId
              AND request.permission_start <= :date
              AND (request.permission_end >= :date OR request.permission_end IS NULL)
              AND request.status IN ('ACCEPTED', 'FULFILLED')
            """, nativeQuery = true)
    List<AtPermissionRequestProjection> findAcceptedAndFulfilledByMeteringPointIdAndDate(
            @Param("meteringPointId") String meteringPointId, @Param("date") LocalDate date);

    @Override
    @Query(value = """
            SELECT request.*, request.cm_consent_id AS consent_id
            FROM at_eda.eda_permission_request request
            WHERE request.cm_consent_id = :consentId
            """, nativeQuery = true)
    Optional<AtPermissionRequestProjection> findByConsentId(@Param("consentId") String consentId);

    @Override
    List<AtPermissionRequest> findByStatusIn(Set<PermissionProcessStatus> status);

    @Query(value = """
            SELECT request.*, request.cm_consent_id AS consent_id
            FROM at_eda.eda_permission_request request
            WHERE request.metering_point_id = :meteringPointId
              AND request.permission_start <= :date
              AND (request.permission_end >= :date OR request.permission_end IS NULL)
              AND request.status IN ('ACCEPTED', 'FULFILLED', 'SENT_TO_PERMISSION_ADMINISTRATOR')
            """, nativeQuery = true)
    List<AtPermissionRequestProjection> findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted(
            String meteringPointId, LocalDate date);
}
