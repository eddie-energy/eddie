CREATE TABLE cds.cds_server
(
    id            SERIAL PRIMARY KEY,
    base_uri      text         NOT NULL UNIQUE,
    name          text         NOT NULL,
    client_id     varchar(255) NOT NULL,
    client_secret varchar(255) NOT NULL
);

CREATE TABLE cds.coverage
(
    cds_server_id INT  NOT NULL,
    energy_type   text NOT NULL,
    PRIMARY KEY (cds_server_id, energy_type),
    FOREIGN KEY (cds_server_id) REFERENCES cds_server (id)
)