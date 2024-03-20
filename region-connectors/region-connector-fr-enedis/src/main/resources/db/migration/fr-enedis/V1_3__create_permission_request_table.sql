ALTER TABLE fr_enedis.enedis_permission_request
    ALTER COLUMN start_timestamp TYPE date,
    ALTER COLUMN end_timestamp TYPE date;

ALTER TABLE fr_enedis.enedis_permission_request
    RENAME COLUMN start_timestamp TO start_date;

ALTER TABLE fr_enedis.enedis_permission_request
    RENAME COLUMN end_timestamp TO end_date;
