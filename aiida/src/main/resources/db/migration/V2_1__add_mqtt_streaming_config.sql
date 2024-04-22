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
