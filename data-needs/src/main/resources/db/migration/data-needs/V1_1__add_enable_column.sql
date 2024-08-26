ALTER TABLE accounting_point_data_need
    ADD enabled boolean DEFAULT TRUE;

ALTER TABLE generic_aiida_data_need
    ADD enabled boolean DEFAULT TRUE;

ALTER TABLE smart_meter_aiida_data_need
    ADD enabled boolean DEFAULT TRUE;

ALTER TABLE validated_consumption_data_need
    ADD enabled boolean DEFAULT TRUE;
