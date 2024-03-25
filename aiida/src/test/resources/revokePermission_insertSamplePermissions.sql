-- Insert one permission that is eligible for revocation and one that is not.

INSERT INTO public.kafka_streaming_config (id, bootstrap_servers, data_topic, status_topic, subscribe_topic)
VALUES (2, 'localhost:9092', 'ValidPublishTopic', 'ValidStatusTopic', 'ValidSubscribeTopic'),
       (3, 'localhost:9092', 'ValidPublishTopic', 'ValidStatusTopic', 'ValidSubscribeTopic');

INSERT INTO public.permission (permission_id, connection_id, data_need_id, expiration_time, grant_time, revoke_time,
                               service_name,
                               start_time, status, kafka_streaming_config_id)
VALUES ('592c372e-bced-45b7-a4a9-5f39e66b8d30', 'NewAiidaRandomConnectionId', 'DataNeedId',
        '2023-09-23 16:52:19.533439 +00:00',
        '2023-09-13 08:32:19.533440 +00:00', NULL, 'BlaService', '2023-09-14 10:38:59.533439 +00:00', 'FULFILLED', 2),
       ('1a1c5995-71fc-4078-acd3-46027a2faa51', 'NewAiidaRandomConnectionId', 'DataNeedId',
        '2023-09-23 16:52:19.533439 +00:00',
        '2023-09-13 06:08:59.533440 +00:00', NULL, 'BlaService2', '2023-09-14 10:38:59.533439 +00:00', 'ACCEPTED', 3);

INSERT INTO public.permission_requested_codes (permission_id, code)
VALUES ('592c372e-bced-45b7-a4a9-5f39e66b8d30', '1.8.0'),
       ('592c372e-bced-45b7-a4a9-5f39e66b8d30', '2.8.0'),
       ('1a1c5995-71fc-4078-acd3-46027a2faa51', '1.8.0'),
       ('1a1c5995-71fc-4078-acd3-46027a2faa51', '2.8.0');
