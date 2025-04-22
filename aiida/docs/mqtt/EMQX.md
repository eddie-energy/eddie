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
2. Set the environment variables in the .env for the emqx username and password
   ```text
   EMQX_DATASOURCE_USERNAME=emqx
   EMQX_DATASOURCE_PASSWORD=REPLACE_ME
   ```