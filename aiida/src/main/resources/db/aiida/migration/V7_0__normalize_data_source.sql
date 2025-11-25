ALTER TABLE public.data_source
    DROP CONSTRAINT data_source_image_id_fkey;
ALTER TABLE public.data_source
    RENAME TO data_source_old;

CREATE TABLE public.data_source
(
    id           uuid PRIMARY KEY,
    user_id      uuid                                    NOT NULL,
    asset        TEXT                                    NOT NULL,
    country_code TEXT CHECK ( LENGTH(country_code) = 2 ) NOT NULL,
    enabled      BOOLEAN DEFAULT FALSE,
    icon         TEXT                                    NOT NULL,
    image_id     uuid REFERENCES image (id),
    name         TEXT                                    NOT NULL,
    type         TEXT                                    NOT NULL
);

INSERT INTO public.data_source (id, user_id, asset, country_code, enabled, icon, image_id, name, type)
SELECT id,
       user_id,
       asset,
       country_code,
       enabled,
       icon,
       image_id,
       name,
       data_source_type
FROM public.data_source_old;

CREATE TABLE public.data_source_mqtt_acl
(
    id       BIGSERIAL PRIMARY KEY,
    acl_type TEXT NOT NULL CHECK ( LENGTH(acl_type) <= 5 ),
    action   TEXT NOT NULL CHECK ( LENGTH(action) <= 9 ),
    topic    TEXT NOT NULL,
    username TEXT NOT NULL
);

CREATE TABLE public.data_source_mqtt_user
(
    id       BIGSERIAL PRIMARY KEY,
    password TEXT NOT NULL,
    username TEXT NOT NULL
);

CREATE TABLE public.data_source_mqtt
(
    id            uuid PRIMARY KEY REFERENCES data_source (id),
    external_host TEXT NOT NULL,
    internal_host TEXT NOT NULL,
    mqtt_user_id  BIGINT REFERENCES data_source_mqtt_user (id),
    mqtt_acl_id   BIGINT REFERENCES data_source_mqtt_acl (id)
);

CREATE TABLE public.data_source_mqtt_inbound
(
    id          uuid PRIMARY KEY REFERENCES data_source_mqtt (id),
    access_code TEXT NOT NULL
);

CREATE TABLE public.data_source_interval
(
    id               uuid PRIMARY KEY REFERENCES data_source (id),
    polling_interval INT NOT NULL
);

CREATE TABLE public.data_source_modbus
(
    id         uuid PRIMARY KEY REFERENCES data_source_interval (id),
    device_id  uuid NOT NULL,
    ip_address TEXT NOT NULL CHECK ( LENGTH(ip_address) <= 15 ),
    model_id   uuid NOT NULL,
    vendor_id  uuid NOT NULL
);


INSERT INTO public.data_source_mqtt (id, external_host, internal_host)
SELECT id,
       external_host,
       internal_host
FROM public.data_source_old
WHERE data_source_type IN
      ('SMART_METER_ADAPTER', 'MICRO_TELEINFO', 'SINAPSI_ALFA', 'SMART_GATEWAYS_ADAPTER', 'SHELLY', 'INBOUND',
       'CIM_ADAPTER');

INSERT INTO public.data_source_mqtt_inbound (id, access_code)
SELECT id,
       access_code
FROM public.data_source_old
WHERE data_source_type = 'INBOUND';

WITH inserted_users AS (
    INSERT INTO public.data_source_mqtt_user (username, password)
        SELECT username, password
        FROM public.data_source_old
        WHERE data_source_type IN (
                                   'SMART_METER_ADAPTER', 'MICRO_TELEINFO', 'SINAPSI_ALFA',
                                   'SMART_GATEWAYS_ADAPTER', 'SHELLY', 'INBOUND', 'CIM_ADAPTER'
            )
        RETURNING id AS mqtt_user_id),
     source_ids AS (SELECT id                   AS data_source_id,
                           ROW_NUMBER() OVER () AS rn
                    FROM public.data_source_old
                    WHERE data_source_type IN (
                                               'SMART_METER_ADAPTER', 'MICRO_TELEINFO', 'SINAPSI_ALFA',
                                               'SMART_GATEWAYS_ADAPTER', 'SHELLY', 'INBOUND', 'CIM_ADAPTER'
                        )),
     user_ids AS (SELECT mqtt_user_id,
                         ROW_NUMBER() OVER () AS rn
                  FROM inserted_users)
UPDATE public.data_source_mqtt dsm
SET mqtt_user_id = user_ids.mqtt_user_id
FROM source_ids
         JOIN user_ids ON source_ids.rn = user_ids.rn
WHERE dsm.id = source_ids.data_source_id;

WITH inserted_acls AS (
    INSERT INTO public.data_source_mqtt_acl (acl_type, action, topic, username)
        SELECT acl_type, action, topic, username
        FROM public.data_source_old
        WHERE data_source_type IN (
                                   'SMART_METER_ADAPTER', 'MICRO_TELEINFO', 'SINAPSI_ALFA',
                                   'SMART_GATEWAYS_ADAPTER', 'SHELLY', 'INBOUND', 'CIM_ADAPTER'
            )
        RETURNING id AS mqtt_acl_id),
     source_ids AS (SELECT id                   AS data_source_id,
                           ROW_NUMBER() OVER () AS rn
                    FROM public.data_source_old
                    WHERE data_source_type IN (
                                               'SMART_METER_ADAPTER', 'MICRO_TELEINFO', 'SINAPSI_ALFA',
                                               'SMART_GATEWAYS_ADAPTER', 'SHELLY', 'INBOUND', 'CIM_ADAPTER'
                        )),
     acl_ids AS (SELECT mqtt_acl_id,
                        ROW_NUMBER() OVER () AS rn
                 FROM inserted_acls)
UPDATE public.data_source_mqtt dsm
SET mqtt_acl_id = acl_ids.mqtt_acl_id
FROM source_ids
         JOIN acl_ids ON source_ids.rn = acl_ids.rn
WHERE dsm.id = source_ids.data_source_id;


INSERT INTO public.data_source_interval (id, polling_interval)
SELECT id,
       polling_interval
FROM public.data_source_old
WHERE data_source_type IN ('SIMULATION', 'MODBUS');

INSERT INTO public.data_source_modbus (id, device_id, ip_address, model_id, vendor_id)
SELECT id,
       modbus_device,
       modbus_ip,
       modbus_model,
       modbus_vendor
FROM public.data_source_old
WHERE data_source_type = 'MODBUS';

ALTER TABLE public.permission
    ADD CONSTRAINT fk_permission_to_data_source FOREIGN KEY (data_source_id) REFERENCES public.data_source (id);