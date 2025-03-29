DROP TABLE IF EXISTS cds.coverage;
DROP TABLE IF EXISTS cds.cds_server;
CREATE TABLE cds.cds_server
(
    id                                    SERIAL PRIMARY KEY,
    base_uri                              text         NOT NULL UNIQUE,
    name                                  text         NOT NULL,
    admin_client_id                       varchar(255) NOT NULL,
    admin_client_secret                   varchar(255) NOT NULL,
    pushed_authorization_request_endpoint varchar(255) NOT NULL,
    authorization_endpoint                varchar(255) NOT NULL,
    token_endpoint                        varchar(255) NOT NULL,
    clients_endpoint                      varchar(255) NOT NULL,
    credentials_endpoint                  varchar(255) NOT NULL,
    accounts_endpoint                     varchar(255) NOT NULL,
    service_contracts_endpoint            varchar(255) NOT NULL,
    service_points_endpoint               varchar(255) NOT NULL,
    meter_device_endpoint                 varchar(255) NOT NULL,
    bill_section_endpoint                 varchar(255) NOT NULL,
    usage_point_endpoint                  varchar(255) NOT NULL,
    customer_data_client_id               VARCHAR(255) NOT NULL,
    customer_data_client_secret           VARCHAR(255) NOT NULL
);

CREATE TABLE cds.coverage
(
    cds_server_id INT        NOT NULL,
    energy_type   text       NOT NULL,
    country_code  varchar(2) NOT NULL,
    PRIMARY KEY (cds_server_id, energy_type),
    FOREIGN KEY (cds_server_id) REFERENCES cds_server (id)
);
