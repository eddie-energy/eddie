ALTER TABLE nl_mijn_aansluiting.permission_event
    ADD COLUMN postal_code  VARCHAR(6), -- See postal code system of the netherlands: https://umbrex.com/resources/how-to-address-an-international-letter-to-any-country/how-to-address-an-international-letter-to-netherlands/
    ADD COLUMN house_number VARCHAR(31);

CREATE OR REPLACE VIEW nl_mijn_aansluiting.permission_request AS
SELECT DISTINCT ON (permission_id) permission_id,
                                   nl_mijn_aansluiting.firstval_agg(connection_id) OVER w                 AS connection_id,
                                   nl_mijn_aansluiting.firstval_agg(created) OVER w                       AS created,
                                   nl_mijn_aansluiting.firstval_agg(data_need_id) OVER w                  AS data_need_id,
                                   nl_mijn_aansluiting.firstval_agg(granularity) OVER w                   AS granularity,
                                   nl_mijn_aansluiting.firstval_agg(permission_start) OVER w              AS permission_start,
                                   nl_mijn_aansluiting.firstval_agg(permission_end) OVER w                AS permission_end,
                                   nl_mijn_aansluiting.firstval_agg(status) OVER w                        AS status,
                                   nl_mijn_aansluiting.firstval_agg(state) OVER w                         AS state,
                                   nl_mijn_aansluiting.firstval_agg(code_verifier) OVER w                 AS code_verifier,
                                   nl_mijn_aansluiting.firstval_agg(permission_event.postal_code) OVER w  AS postal_code,
                                   nl_mijn_aansluiting.firstval_agg(permission_event.house_number)
                                   OVER w                                                                 AS house_number
FROM nl_mijn_aansluiting.permission_event
WINDOW w AS (PARTITION BY permission_id ORDER BY event_created DESC)
ORDER BY permission_id, event_created;
