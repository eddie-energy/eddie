UPDATE aiida.aiida_mqtt_acl
SET topic = topic || '/+'
WHERE topic LIKE 'aiida/v1/%/data/outbound' OR topic LIKE 'aiida/v1/%/data/outbound';