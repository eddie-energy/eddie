INSERT INTO public.kafka_streaming_config (id, bootstrap_servers, data_topic, status_topic, subscribe_topic)
VALUES (102, 'localhost:9093', 'ValidPublishTopic1', 'ValidStatusTopic1', 'ValidSubscribeTopic1'),
       (152, 'localhost:9093', 'ValidPublishTopic1', 'ValidStatusTopic1', 'ValidSubscribeTopic1'),
       (202, 'localhost:9092', 'ValidPublishTopic', 'ValidStatusTopic', 'ValidSubscribeTopic');

INSERT INTO public.permission (permission_id, connection_id, data_need_id, expiration_time, grant_time, revoke_time,
                               service_name, start_time, status, kafka_streaming_config_id)
VALUES ('25ee5365-5d71-4b01-b21f-9c61f76a5cc9', 'TEST', 'DataNeedId', '2023-12-24 03:47:20.650669 +00:00',
        '2023-09-13 08:52:20.650679 +00:00', NULL, 'Test IntelliJ', '2023-09-14 12:39:00.650669 +00:00', 'ACCEPTED',
        102),
       ('9609a9b3-0718-4082-935d-6a98c0f8c5a2', 'TEST2', 'DataNeedId', '2023-09-19 03:47:20.650669 +00:00',
        '2023-09-13 08:52:20.650679 +00:00', NULL, 'Test IntelliJ2', '2023-09-14 12:39:00.650669 +00:00',
        'WAITING_FOR_START', 152),
       ('0b3b6f6d-d878-49dd-9dfd-62156b5cdc37', 'NewAiidaRandomConnectionId', 'DataNeedId',
        '2023-09-30 13:39:52.610385 +00:00',
        '2023-09-20 13:39:42.610389 +00:00', NULL, 'My NewAIIDA Test Service', '2023-09-20 13:39:42.610385 +00:00',
        'STREAMING_DATA', 202);

INSERT INTO public.permission_requested_codes (permission_id, code)
VALUES ('25ee5365-5d71-4b01-b21f-9c61f76a5cc9', '2.8.0'),
       ('25ee5365-5d71-4b01-b21f-9c61f76a5cc9', '1.8.0'),
       ('9609a9b3-0718-4082-935d-6a98c0f8c5a2', '1.8.0'),
       ('0b3b6f6d-d878-49dd-9dfd-62156b5cdc37', '1.8.0'),
       ('9609a9b3-0718-4082-935d-6a98c0f8c5a2', '2.8.0'),
       ('0b3b6f6d-d878-49dd-9dfd-62156b5cdc37', '2.8.0');
