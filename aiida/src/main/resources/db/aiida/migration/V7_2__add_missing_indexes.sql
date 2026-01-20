/* ============================================================
   FOREIGN KEY SUPPORTING INDEXES
   ============================================================ */

CREATE INDEX IF NOT EXISTS idx_aiida_record_value__aiida_record_id
    ON aiida_record_value (aiida_record_id);

CREATE INDEX IF NOT EXISTS idx_data_source__image_id
    ON data_source (image_id);

CREATE INDEX IF NOT EXISTS idx_data_source_mqtt__user_id
    ON data_source_mqtt (mqtt_user_id);

CREATE INDEX IF NOT EXISTS idx_data_source_mqtt__acl_id
    ON data_source_mqtt (mqtt_acl_id);

CREATE INDEX IF NOT EXISTS idx_failed_to_send_entity__permission_id
    ON failed_to_send_entity (permission_id);

CREATE INDEX IF NOT EXISTS idx_mqtt_streaming_config__permission_id
    ON mqtt_streaming_config (permission_id);

CREATE INDEX IF NOT EXISTS idx_permission__data_source_id
    ON permission (data_source_id);

CREATE INDEX IF NOT EXISTS idx_permission__data_need_id
    ON permission (data_need_id);


/* ============================================================
   QUERY OPTIMIZATION INDEXES
   ============================================================ */

CREATE INDEX IF NOT EXISTS idx_aiida_record__data_source_id_id_desc
    ON aiida_record (data_source_id, id DESC);

CREATE INDEX IF NOT EXISTS idx_data_source__user_id
    ON data_source (user_id);

CREATE INDEX IF NOT EXISTS idx_inbound_record__data_source_id_timestamp_desc
    ON inbound_record (data_source_id, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_inbound_record__timestamp
    ON inbound_record (timestamp);

CREATE INDEX IF NOT EXISTS idx_aiida_record__timestamp
    ON aiida_record (timestamp);

CREATE INDEX IF NOT EXISTS idx_failed_to_send_entity__created_at
    ON failed_to_send_entity (created_at);

CREATE INDEX IF NOT EXISTS idx_data_source_mqtt_user__username
    ON data_source_mqtt_user (username);

CREATE INDEX IF NOT EXISTS idx_data_source_mqtt_acl__username
    ON data_source_mqtt_acl (username);

CREATE INDEX IF NOT EXISTS idx_permission__user_id_grant_time_desc
    ON permission (user_id, grant_time DESC);

CREATE INDEX IF NOT EXISTS idx_permission__status
    ON permission (status);
