INSERT INTO public.permission (permission_id, connection_id, data_need_id, expiration_time, grant_time, revoke_time,
                               service_name,
                               start_time, status)
VALUES ('6ad75c0c-b622-4f4a-b195-3e8cf1f09676', 'NewAiidaRandomConnectionId', 'DataNeedId',
        '2023-09-23 18:20:18.396911 +00:00',
        '2023-09-13 12:00:00.000000 +00:00', NULL, 'Service3', '2023-09-14 12:06:58.396911 +00:00', 'ACCEPTED'),
       ('d8dcfee0-2a3c-4974-866f-fefaead06345', 'NewAiidaRandomConnectionId', 'DataNeedId',
        '2023-09-23 18:20:18.396911 +00:00',
        '2023-09-13 08:00:00.000000 +00:00', NULL, 'Service2', '2023-09-14 12:06:58.396911 +00:00', 'ACCEPTED'),
       ('4fff4b2e-2211-45c5-aff5-b504c24f20d1', 'NewAiidaRandomConnectionId', 'DataNeedId',
        '2023-09-23 18:20:18.396911 +00:00',
        '2023-09-13 10:00:00.000000 +00:00', NULL, 'Service1', '2023-09-14 12:06:58.396911 +00:00', 'ACCEPTED');


INSERT INTO public.permission_requested_codes (permission_id, code)
VALUES ('6ad75c0c-b622-4f4a-b195-3e8cf1f09676', '1.8.0'),
       ('6ad75c0c-b622-4f4a-b195-3e8cf1f09676', '2.8.0'),
       ('d8dcfee0-2a3c-4974-866f-fefaead06345', '1.8.0'),
       ('d8dcfee0-2a3c-4974-866f-fefaead06345', '2.8.0'),
       ('4fff4b2e-2211-45c5-aff5-b504c24f20d1', '1.8.0'),
       ('4fff4b2e-2211-45c5-aff5-b504c24f20d1', '2.8.0');
