ALTER TABLE enedis_permission_request
    ADD
        COLUMN latest_meter_reading date DEFAULT NULL
