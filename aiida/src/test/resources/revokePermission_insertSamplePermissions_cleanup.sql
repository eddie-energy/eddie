DELETE
FROM public.permission_requested_codes
WHERE permission_requested_codes.permission_permission_id IN
      ('592c372e-bced-45b7-a4a9-5f39e66b8d30', '1a1c5995-71fc-4078-acd3-46027a2faa51');

DELETE
FROM public.permission
WHERE permission.permission_id IN ('592c372e-bced-45b7-a4a9-5f39e66b8d30', '1a1c5995-71fc-4078-acd3-46027a2faa51');

DELETE
FROM public.kafka_streaming_config
WHERE kafka_streaming_config.id IN (2, 3);