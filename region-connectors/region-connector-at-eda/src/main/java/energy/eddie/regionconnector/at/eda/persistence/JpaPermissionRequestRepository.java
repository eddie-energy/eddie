// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
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
        WITH permissions AS (
            SELECT DISTINCT ON (permission_id) permission_id
            FROM at_eda.permission_event
            WHERE cm_request_id = :cmRequestId or conversation_id = :conversationId
        )
        SELECT DISTINCT ON (pe.permission_id)
            pe.permission_id,
            at_eda.firstval_agg(pe.cm_request_id)     OVER w AS cm_request_id,
            at_eda.firstval_agg(pe.connection_id)     OVER w AS connection_id,
            at_eda.firstval_agg(pe.conversation_id)   OVER w AS conversation_id,
            at_eda.firstval_agg(pe.created)           OVER w AS created,
            at_eda.firstval_agg(pe.data_need_id)      OVER w AS data_need_id,
            at_eda.firstval_agg(pe.dso_id)            OVER w AS dso_id,
            at_eda.firstval_agg(pe.granularity)       OVER w AS granularity,
            at_eda.firstval_agg(pe.metering_point_id) OVER w AS metering_point_id,
            at_eda.firstval_agg(pe.permission_start)  OVER w AS permission_start,
            at_eda.firstval_agg(pe.permission_end)    OVER w AS permission_end,
            at_eda.firstval_agg(pe.cm_consent_id)     OVER w AS cm_consent_id,
            at_eda.firstval_agg(pe.message)           OVER w AS message,
            at_eda.firstval_agg(pe.status)            OVER w AS status,
            at_eda.firstval_agg(pe.cause)             OVER w AS cause
        FROM at_eda.permission_event pe
                 JOIN permissions USING (permission_id)
        WINDOW w AS (
                PARTITION BY pe.permission_id
                ORDER BY pe.event_created DESC
                ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                )
        ORDER BY pe.permission_id, pe.event_created DESC;
        """, nativeQuery = true)
    List<AtPermissionRequestProjection> findByConversationIdOrCMRequestId(
            @Param("conversationId") String conversationId,
            @Param("cmRequestId") @Nullable String cmRequestId
    );

    @Override
    @Query("SELECT pr FROM EdaPermissionRequest pr WHERE pr.conversationId = :conversationId AND pr.meteringPointId = :meteringPointId")
    Optional<AtPermissionRequest> findByConversationIdAndMeteringPointId(
            @Param("conversationId") String conversationId,
            @Param("meteringPointId") @Nullable String meteringPointId
    );

    @Override
    @Query(value = """
             WITH permissions_for_metering_point AS (SELECT DISTINCT ON (permission_id) permission_id
                                                     FROM at_eda.permission_event
                                                     WHERE metering_point_id = :meteringPointId),
              permission_result AS (SELECT DISTINCT ON (pe.permission_id) pe.permission_id,
                                      at_eda.firstval_agg(cm_request_id) OVER w     AS cm_request_id,
                                      at_eda.firstval_agg(connection_id) OVER w     AS connection_id,
                                      at_eda.firstval_agg(conversation_id) OVER w   AS conversation_id,
                                      at_eda.firstval_agg(created) OVER w           AS created,
                                      at_eda.firstval_agg(data_need_id) OVER w      AS data_need_id,
                                      at_eda.firstval_agg(dso_id) OVER w            AS dso_id,
                                      at_eda.firstval_agg(granularity) OVER w       AS granularity,
                                      at_eda.firstval_agg(metering_point_id) OVER w AS metering_point_id,
                                      at_eda.firstval_agg(permission_start) OVER w  AS permission_start,
                                      at_eda.firstval_agg(permission_end) OVER w    AS permission_end,
                                      at_eda.firstval_agg(cm_consent_id) OVER w     AS cm_consent_id,
                                      at_eda.firstval_agg(message) OVER w           AS message,
                                      at_eda.firstval_agg(status) OVER w            AS status,
                                      at_eda.firstval_agg(cause) OVER w             AS cause
                                    FROM at_eda.permission_event pe, permissions_for_metering_point pm
                                    WHERE pe.permission_id = pm.permission_id
                                    WINDOW w AS (PARTITION BY pe.permission_id ORDER BY event_created DESC)
                                    ORDER BY pe.permission_id, pe.event_created)
             SELECT pr
             FROM permission_result pr
             where pr.permission_start <= :date
               AND (pr.permission_end >= :date OR pr.permission_end IS NULL)
               AND pr.status IN ('ACCEPTED','FULFILLED');
            """, nativeQuery = true)
    List<AtPermissionRequestProjection> findAcceptedAndFulfilledByMeteringPointIdAndDate(
            @Param("meteringPointId") String meteringPointId,
            @Param("date") LocalDate date
    );

    @Override
    @Query(value = """
        WITH permissions AS (
            SELECT DISTINCT ON (permission_id) permission_id
            FROM at_eda.permission_event
            WHERE cm_consent_id = :consentId
        )
        SELECT DISTINCT ON (pe.permission_id)
            pe.permission_id,
            at_eda.firstval_agg(pe.cm_request_id)     OVER w AS cm_request_id,
            at_eda.firstval_agg(pe.connection_id)     OVER w AS connection_id,
            at_eda.firstval_agg(pe.conversation_id)   OVER w AS conversation_id,
            at_eda.firstval_agg(pe.created)           OVER w AS created,
            at_eda.firstval_agg(pe.data_need_id)      OVER w AS data_need_id,
            at_eda.firstval_agg(pe.dso_id)            OVER w AS dso_id,
            at_eda.firstval_agg(pe.granularity)       OVER w AS granularity,
            at_eda.firstval_agg(pe.metering_point_id) OVER w AS metering_point_id,
            at_eda.firstval_agg(pe.permission_start)  OVER w AS permission_start,
            at_eda.firstval_agg(pe.permission_end)    OVER w AS permission_end,
            at_eda.firstval_agg(pe.cm_consent_id)     OVER w AS cm_consent_id,
            at_eda.firstval_agg(pe.message)           OVER w AS message,
            at_eda.firstval_agg(pe.status)            OVER w AS status,
            at_eda.firstval_agg(pe.cause)             OVER w AS cause
        FROM at_eda.permission_event pe
                 JOIN permissions USING (permission_id)
        WINDOW w AS (
                PARTITION BY pe.permission_id
                ORDER BY pe.event_created DESC
                ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                )
        ORDER BY pe.permission_id, pe.event_created DESC;
        """, nativeQuery = true)
    Optional<AtPermissionRequestProjection> findByConsentId(@Param("consentId") String consentId);

    @Override
    List<AtPermissionRequest> findByStatusIn(Set<PermissionProcessStatus> status);

    /**
     * This method returns all permission requests that are associated with the given metering point, where:
     * <ul style="bullet">
     *     <li>the date is between start and end of the permission request</li>
     *     <li>the state is either {@link energy.eddie.api.v0.PermissionProcessStatus#SENT_TO_PERMISSION_ADMINISTRATOR} or after {@link energy.eddie.api.v0.PermissionProcessStatus#ACCEPTED} </li>
     * </ul>
     * For more info about the states consult "permission-process-model.md"
     *
     * @param meteringPointId for which to get permission requests
     * @param date            to filter time relevant permission requests
     * @return a list of matching permission requests
     */
    @Query(value = """
            WITH permissions_for_metering_point AS (SELECT DISTINCT ON (permission_id) permission_id
                                                FROM at_eda.permission_event
                                                WHERE metering_point_id = :meteringPointId),
        permission_result AS (SELECT DISTINCT ON (pe.permission_id) pe.permission_id,
                at_eda.firstval_agg(cm_request_id) OVER w     AS cm_request_id,
                at_eda.firstval_agg(connection_id) OVER w     AS connection_id,
                at_eda.firstval_agg(conversation_id) OVER w   AS conversation_id,
                at_eda.firstval_agg(created) OVER w           AS created,
                at_eda.firstval_agg(data_need_id) OVER w      AS data_need_id,
                at_eda.firstval_agg(dso_id) OVER w            AS dso_id,
                at_eda.firstval_agg(granularity) OVER w       AS granularity,
                at_eda.firstval_agg(metering_point_id) OVER w AS metering_point_id,
                at_eda.firstval_agg(permission_start) OVER w  AS permission_start,
                at_eda.firstval_agg(permission_end) OVER w    AS permission_end,
                at_eda.firstval_agg(cm_consent_id) OVER w     AS cm_consent_id,
                at_eda.firstval_agg(message) OVER w           AS message,
                at_eda.firstval_agg(status) OVER w            AS status,
                at_eda.firstval_agg(cause) OVER w             AS cause
        FROM at_eda.permission_event pe, permissions_for_metering_point pm
        WHERE pe.permission_id = pm.permission_id
        WINDOW w AS (PARTITION BY pe.permission_id ORDER BY event_created DESC)
        ORDER BY pe.permission_id, pe.event_created)
        SELECT permission_id,
               cm_request_id,
               connection_id,
               conversation_id,
               created,
               data_need_id,
               dso_id,
               granularity,
               metering_point_id,
               permission_start,
               permission_end,
               cm_consent_id,
               message,
               status,
               cause
        FROM permission_result
        where permission_start <= :date
          AND (permission_end >= :date OR permission_end IS NULL)
          AND status IN (
              'ACCEPTED',
              'FULFILLED',
              'SENT_TO_PERMISSION_ADMINISTRATOR'
          );
    """, nativeQuery = true)
    List<AtPermissionRequestProjection> findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted(String meteringPointId, LocalDate date);
}
