CREATE TABLE public.aiida_record
(
);

CREATE TABLE public.double_aiida_record
(
);

CREATE TABLE public.integer_aiida_record
(
);

CREATE TABLE public.kafka_streaming_config
(
    id                bigint PRIMARY KEY     NOT NULL,
    bootstrap_servers character varying(255) NOT NULL,
    data_topic        character varying(255) NOT NULL,
    status_topic      character varying(255) NOT NULL,
    subscribe_topic   character varying(255) NOT NULL
);

CREATE TABLE public.permission
(
    permission_id             character varying(255) PRIMARY KEY NOT NULL,
    connection_id             character varying(255)             NOT NULL,
    expiration_time           timestamp(6) WITH TIME ZONE        NOT NULL,
    grant_time                timestamp(6) WITH TIME ZONE        NOT NULL,
    revoke_time               timestamp(6) WITH TIME ZONE,
    service_name              character varying(255)             NOT NULL,
    start_time                timestamp(6) WITH TIME ZONE        NOT NULL,
    status                    character varying(255)             NOT NULL,
    kafka_streaming_config_id bigint                             NOT NULL,
    FOREIGN KEY (kafka_streaming_config_id) REFERENCES public.kafka_streaming_config (id)
        MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);
CREATE UNIQUE INDEX uk_t96h9igj8ghjmtl5vqmr9vtqm ON permission USING btree (kafka_streaming_config_id);

CREATE TABLE public.permission_requested_codes
(
    permission_permission_id character varying(255) NOT NULL,
    requested_codes          character varying(255) NOT NULL,
    PRIMARY KEY (permission_permission_id, requested_codes),
    FOREIGN KEY (permission_permission_id) REFERENCES public.permission (permission_id)
        MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE public.string_aiida_record
(
);

