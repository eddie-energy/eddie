GRANT USAGE ON SCHEMA aiida TO emqx;
GRANT SELECT ON aiida.aiida_mqtt_acl TO emqx;
GRANT SELECT ON aiida.aiida_mqtt_user TO emqx;
GRANT CONNECT ON DATABASE eddie TO emqx;