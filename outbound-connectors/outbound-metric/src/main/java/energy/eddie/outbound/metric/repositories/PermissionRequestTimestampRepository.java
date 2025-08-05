package energy.eddie.outbound.metric.repositories;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.model.PersistablePermissionEvent;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Repository
public class PermissionRequestTimestampRepository {

    private final EntityManager entityManager;

    public PermissionRequestTimestampRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PersistablePermissionEvent getPermissionRequestTimestamp(String permissionId, String tableName) {
        Object[] result = (Object[]) entityManager.createNativeQuery(String.format("""
        WITH ordered_permission_request_events AS (
            SELECT permission_id, status, event_created,
                ROW_NUMBER() OVER (PARTITION BY permission_id ORDER BY event_created DESC) AS rn
            FROM %s
            WHERE permission_id = :permissionId
        )
        SELECT permission_id, status, event_created
        FROM ordered_permission_request_events
        WHERE rn = 2;
    """, tableName))
                .setParameter("permissionId", permissionId)
                .getSingleResult();

        String eventStatus = (String) result[1];
        Instant eventCreated = (Instant) result[2];
        ZonedDateTime eventTimestamp = eventCreated.atZone(ZoneOffset.UTC);

        PermissionProcessStatus status = PermissionProcessStatus.valueOf(eventStatus);

        return new PersistablePermissionEvent(permissionId, status, eventTimestamp);
    }
}
