SET SCHEMA 'at_eda';

ALTER TABLE at_eda.permission_event
    ADD COLUMN meter_reading_start timestamp(6) with time zone,
    ADD COLUMN meter_reading_end   timestamp(6) with time zone;

CREATE VIEW meter_reading_timeframe AS
WITH ordered_data AS (SELECT permission_id,
                             meter_reading_start,
                             meter_reading_end,
                             LAG(permission_event.meter_reading_start)
                             OVER (PARTITION BY permission_id ORDER BY meter_reading_end, meter_reading_start) AS prev_start_date,
                             LAG(meter_reading_end)
                             OVER (PARTITION BY permission_id ORDER BY meter_reading_end, meter_reading_start) AS prev_end_date
                      FROM at_eda.permission_event
                      WHERE dtype = 'DataReceivedEvent' -- Only get the corresponding events to not iterate over all events
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
