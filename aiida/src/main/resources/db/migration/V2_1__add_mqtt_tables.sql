CREATE TABLE mqtt_streaming_config
(
    permission_id     varchar(36) NOT NULL PRIMARY KEY,
    data_topic        text        NOT NULL,
    password          text        NOT NULL,
    server_uri        text        NOT NULL,
    status_topic      text        NOT NULL,
    termination_topic text        NOT NULL,
    username          text        NOT NULL
);


-- manually create sequence so hibernate can find it
CREATE SEQUENCE failed_to_send_entity_seq;

CREATE TABLE failed_to_send_entity
(
    id            integer     NOT NULL DEFAULT NEXTVAL('failed_to_send_entity_seq') PRIMARY KEY,
    permission_id varchar(36) NOT NULL REFERENCES permission (permission_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    topic         text                        NOT NULL,
    json_value    bytea                       NOT NULL,
    created_at    timestamp(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER SEQUENCE failed_to_send_entity_seq
    OWNED BY failed_to_send_entity.id;
