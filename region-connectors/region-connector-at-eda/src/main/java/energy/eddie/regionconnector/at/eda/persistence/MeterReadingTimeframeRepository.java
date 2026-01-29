// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.regionconnector.at.eda.permission.request.projections.MeterReadingTimeframe;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MeterReadingTimeframeRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public MeterReadingTimeframeRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<MeterReadingTimeframe> findAllByPermissionId(String permissionId) {
        String permissionsQuery = """
        WITH ordered_data AS (SELECT permission_id,
                                     meter_reading_start,
                                     meter_reading_end,
                                     LAG(permission_event.meter_reading_start)
                                     OVER (PARTITION BY permission_id ORDER BY meter_reading_end, meter_reading_start) AS prev_start_date,
                                     LAG(meter_reading_end)
                                     OVER (PARTITION BY permission_id ORDER BY meter_reading_end, meter_reading_start) AS prev_end_date
                              FROM at_eda.permission_event
                              WHERE dtype = 'DataReceivedEvent' -- Only get the corresponding events to not iterate over all events
                                AND permission_id = :permissionId
                                AND permission_event.meter_reading_end IS NOT NULL
                                AND permission_event.meter_reading_start IS NOT NULL),
             grouped_data AS (
                 -- Create a group when the current row is NOT consecutive or overlapping with the previous row
                 SELECT permission_id,
                        meter_reading_start,
                        meter_reading_end,
                        prev_end_date,
                        SUM(
                        CASE
                            WHEN meter_reading_start > prev_end_date + INTERVAL '1 day'
                                THEN 1
                            ELSE 0
                            END
                           ) OVER (PARTITION BY permission_id ORDER BY meter_reading_end, meter_reading_start) AS group_id
                 FROM ordered_data)
        -- Aggregate by group_id to get final merged intervals
        SELECT permission_id,
               MIN(meter_reading_start) AS meter_reading_start,
               MAX(meter_reading_end)   AS meter_reading_end,
               ROW_NUMBER() OVER ()     AS id
        FROM grouped_data
        GROUP BY permission_id, group_id
        ORDER BY permission_id, meter_reading_start, meter_reading_end;
        """;

        List<?> permissions = entityManager
                .createNativeQuery(permissionsQuery, Tuple.class)
                .setParameter("permissionId", permissionId)
                .getResultList();

        List<MeterReadingTimeframe> timeframes = new ArrayList<>();
        for (Object obj : permissions) {
            var tuple = (Tuple) obj;
            timeframes.add(new MeterReadingTimeframe(
                    ((Number) tuple.get("id")).longValue(),
                    tuple.get("permission_id", String.class),
                    tuple.get("meter_reading_start", Instant.class).atZone(ZoneId.systemDefault()).toLocalDate(),
                    tuple.get("meter_reading_end", Instant.class).atZone(ZoneId.systemDefault()).toLocalDate()
            ));
        }

        return timeframes;
    }
}