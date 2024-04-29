CREATE TABLE aiida_mqtt_user
(
    id            varchar(36) NOT NULL PRIMARY KEY,
    username      text        NOT NULL UNIQUE,
    password_hash text        NOT NULL,
    is_superuser  boolean                  DEFAULT FALSE,
    created_at    timestamp WITH TIME ZONE DEFAULT NOW(),
    permission_id varchar(36) NOT NULL
);

CREATE TABLE aiida_mqtt_acl
(
    id         varchar(36)  NOT NULL PRIMARY KEY,
    username   text         NOT NULL,
    action     varchar(9),
    acl_type   varchar(5),
    topic      varchar(255) NOT NULL,
    created_at timestamp WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX mqtt_user_username_index ON aiida_mqtt_user (username);
CREATE INDEX mqtt_acl_username_index ON aiida_mqtt_acl (username);
