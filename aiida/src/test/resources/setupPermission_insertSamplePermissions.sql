insert into public.kafka_streaming_config (id, bootstrap_servers, data_topic, status_topic, subscribe_topic)
values (5, 'localhost:9092', 'ValidPublishTopic', 'ValidStatusTopic', 'ValidSubscribeTopic'),
       (6, 'localhost:9092', 'ValidPublishTopic', 'ValidStatusTopic', 'ValidSubscribeTopic'),
       (7, 'localhost:9092', 'ValidPublishTopic', 'ValidStatusTopic', 'ValidSubscribeTopic');

insert into public.permission (permission_id, connection_id, expiration_time, grant_time, revoke_time, service_name,
                               start_time, status, kafka_streaming_config_id)
values ('6ad75c0c-b622-4f4a-b195-3e8cf1f09676', 'NewAiidaRandomConnectionId', '2023-09-23 18:20:18.396911 +00:00',
        '2023-09-13 12:00:00.000000 +00:00', null, 'Service3', '2023-09-14 12:06:58.396911 +00:00', 'ACCEPTED', 5),
       ('d8dcfee0-2a3c-4974-866f-fefaead06345', 'NewAiidaRandomConnectionId', '2023-09-23 18:20:18.396911 +00:00',
        '2023-09-13 08:00:00.000000 +00:00', null, 'Service2', '2023-09-14 12:06:58.396911 +00:00', 'ACCEPTED', 6),
       ('4fff4b2e-2211-45c5-aff5-b504c24f20d1', 'NewAiidaRandomConnectionId', '2023-09-23 18:20:18.396911 +00:00',
        '2023-09-13 10:00:00.000000 +00:00', null, 'Service1', '2023-09-14 12:06:58.396911 +00:00', 'ACCEPTED', 7);


insert into public.permission_requested_codes (permission_permission_id, requested_codes)
values ('6ad75c0c-b622-4f4a-b195-3e8cf1f09676', '1.8.0'),
       ('6ad75c0c-b622-4f4a-b195-3e8cf1f09676', '2.8.0'),
       ('d8dcfee0-2a3c-4974-866f-fefaead06345', '1.8.0'),
       ('d8dcfee0-2a3c-4974-866f-fefaead06345', '2.8.0'),
       ('4fff4b2e-2211-45c5-aff5-b504c24f20d1', '1.8.0'),
       ('4fff4b2e-2211-45c5-aff5-b504c24f20d1', '2.8.0');