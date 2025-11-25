REVOKE SELECT ON public.data_source FROM emqx;

GRANT SELECT ON public.data_source_mqtt_acl TO emqx;
GRANT SELECT ON public.data_source_mqtt_user TO emqx;