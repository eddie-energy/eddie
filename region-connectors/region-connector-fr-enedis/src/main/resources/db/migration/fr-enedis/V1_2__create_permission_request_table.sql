ALTER TABLE enedis_permission_request
    ADD
        COLUMN created timestamp(6) with time zone DEFAULT NULL
