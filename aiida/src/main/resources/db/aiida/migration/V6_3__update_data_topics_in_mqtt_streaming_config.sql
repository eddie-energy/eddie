UPDATE public.mqtt_streaming_config
SET data_topic = data_topic || 'outbound/+'
WHERE data_topic LIKE 'aiida/v1/%/data';

UPDATE public.mqtt_streaming_config
SET data_topic = data_topic || '/+'
WHERE data_topic LIKE 'aiida/v1/%/data/inbound';