# Documentation for the setup of EMQX MQTT broker

EMQX was decided to be used as the MQTT broker for AIIDA as it can handle the authentication and authorization more easily than NanoMQ.
NanoMQ used HTTP for authentication and authorization, which had limitations.

EMQX uses the `timescaledb` and the `public.data_source` table for authentication and authorization.

The necessary things to do are:

1. Create a user for the EMQX broker with read access to the `public.data_source` table.
   ```postgresql
   CREATE USER emqx WITH PASSWORD 'REPLACE_ME_WITH_SAFE_PASSWORD';
   GRANT USAGE ON SCHEMA public TO emqx;
   GRANT SELECT ON public.data_source TO emqx;
   GRANT CONNECT ON DATABASE aiida TO emqx;
   ```
2. In the EMQX dashboard, add the AIIDA database as source for authentication.
    ```postgresql
    SELECT mqtt_password AS password_hash
    FROM public.data_source
    WHERE mqtt_username = ${username} LIMIT 1;
    ```
3. In the EMQX dashboard, add the AIIDA database as source for authorization.
    ```postgresql
    SELECT LOWER(action) AS action, LOWER(acl_type) AS permission, mqtt_subscribe_topic AS topic
    FROM public.data_source
    WHERE mqtt_username = ${username};
    ```