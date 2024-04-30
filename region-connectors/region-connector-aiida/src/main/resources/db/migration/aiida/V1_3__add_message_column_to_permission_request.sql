ALTER TABLE permission_event
    ADD COLUMN message text DEFAULT NULL;


CREATE OR REPLACE VIEW aiida_permission_request_view AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   firstval_agg(status) OVER w            AS status,
                                   firstval_agg(connection_id) OVER w     AS connection_id,
                                   firstval_agg(data_need_id) OVER w      AS data_need_id,
                                   firstval_agg(permission_start) OVER w  AS permission_start,
                                   firstval_agg(permission_end) OVER w    AS permission_end,
                                   firstval_agg(termination_topic) OVER w AS termination_topic,
                                   firstval_agg(event_created) OVER w     AS created,
                                   firstval_agg(mqtt_username) OVER w     AS mqtt_username,
                                   firstval_agg(message) OVER w           AS message
FROM permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
