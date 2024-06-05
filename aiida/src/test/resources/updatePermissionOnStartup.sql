INSERT INTO public.permission (permission_id, connection_id, data_need_permission_id, expiration_time, grant_time,
                               revoke_time,
                               service_name, start_time, status)
VALUES ('9609a9b3-0718-4082-935d-6a98c0f8c5a2', 'TEST2', 'DataNeedId', '2023-09-19 03:47:20.650669 +00:00',
        '2023-09-13 08:52:20.650679 +00:00', NULL, 'Test IntelliJ2', '2023-09-14 12:39:00.650669 +00:00',
        'WAITING_FOR_START'),
       ('0b3b6f6d-d878-49dd-9dfd-62156b5cdc37', 'NewAiidaRandomConnectionId', 'DataNeedId',
        '2023-09-30 13:39:52.610385 +00:00', '2023-09-20 13:39:42.610389 +00:00', NULL, 'My NewAIIDA Test Service',
        '2023-09-20 13:39:42.610385 +00:00', 'STREAMING_DATA');
