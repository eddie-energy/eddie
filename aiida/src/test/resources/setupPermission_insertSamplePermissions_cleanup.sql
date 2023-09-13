DELETE
FROM public.permission_requested_codes
WHERE permission_requested_codes.permission_permission_id IN
      ('6ad75c0c-b622-4f4a-b195-3e8cf1f09676', 'd8dcfee0-2a3c-4974-866f-fefaead06345',
       '4fff4b2e-2211-45c5-aff5-b504c24f20d1');

DELETE
FROM public.permission
WHERE permission.permission_id IN ('6ad75c0c-b622-4f4a-b195-3e8cf1f09676', 'd8dcfee0-2a3c-4974-866f-fefaead06345',
                                   '4fff4b2e-2211-45c5-aff5-b504c24f20d1');

DELETE
FROM public.kafka_streaming_config
WHERE kafka_streaming_config.id IN (5, 6, 7);