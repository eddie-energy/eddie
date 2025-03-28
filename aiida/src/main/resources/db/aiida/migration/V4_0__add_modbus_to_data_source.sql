ALTER TABLE data_source
    ADD modbus_device UUID,
    ADD modbus_ip VARCHAR(255),
    ADD modbus_model UUID,
    ADD modbus_vendor UUID;