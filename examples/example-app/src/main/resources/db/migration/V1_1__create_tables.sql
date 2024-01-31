CREATE TABLE connection_status
(
    connection_id  VARCHAR(255)             NOT NULL,
    timestamp_     TIMESTAMP WITH TIME ZONE NOT NULL,
    consent_status VARCHAR(48)              NOT NULL
);

CREATE TABLE metering_intervals
(
    metering_interval_secs INTEGER     NOT NULL PRIMARY KEY,
    code                   VARCHAR(16) NOT NULL
);

CREATE TABLE consumption_records
(
    id                     SERIAL PRIMARY KEY,
    connection_id          VARCHAR(255)             NOT NULL,
    metering_point         VARCHAR(255),
    start_date_time        TIMESTAMP WITH TIME ZONE NOT NULL,
    metering_interval_secs INTEGER                  NOT NULL,
    FOREIGN KEY (metering_interval_secs) REFERENCES metering_intervals (metering_interval_secs)
);

CREATE TABLE consumption_points
(
    consumption_record_id INTEGER          NOT NULL,
    ord                   INTEGER          NOT NULL,
    consumption           DOUBLE PRECISION NOT NULL,
    metering_type         VARCHAR(32)      NOT NULL,
    PRIMARY KEY (consumption_record_id, ord),
    FOREIGN KEY (consumption_record_id) REFERENCES consumption_records (id)
);

INSERT INTO metering_intervals (metering_interval_secs, code)
VALUES (3600, 'PT1H'),
       (86400, 'P1D'),
       (1800, 'PT30M'),
       (900, 'PT15M');


CREATE TABLE users
(
    id    SERIAL PRIMARY KEY,
    email VARCHAR(80)
);
CREATE INDEX index_user_email ON users (email);


CREATE TABLE connections
(
    user_id       INTEGER      NOT NULL,
    connection_id VARCHAR(255) NOT NULL PRIMARY KEY
);
CREATE SEQUENCE connection_id_seq AS BIGINT;