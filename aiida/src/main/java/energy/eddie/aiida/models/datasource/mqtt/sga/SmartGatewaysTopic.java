// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.sga;

public enum SmartGatewaysTopic {
    ELECTRICITY_EQUIPMENT_ID("dsmr/reading/electricity_equipment_id"),
    GAS_EQUIPMENT_ID("dsmr/reading/gas_equipment_id"),
    ELECTRICITY_TARIFF("dsmr/reading/electricity_tariff"),
    ELECTRICITY_DELIVERED_1("dsmr/reading/electricity_delivered_1"),
    ELECTRICITY_DELIVERED_2("dsmr/reading/electricity_delivered_2"),
    ELECTRICITY_RETURNED_1("dsmr/reading/electricity_returned_1"),
    ELECTRICITY_RETURNED_2("dsmr/reading/electricity_returned_2"),
    REACTIVE_ELECTRICITY_DELIVERED_1("dsmr/reading/reactive_electricity_delivered_1"),
    REACTIVE_ELECTRICITY_RETURNED_1("dsmr/reading/reactive_electricity_returned_1"),
    REACTIVE_ELECTRICITY_DELIVERED_2("dsmr/reading/reactive_electricity_delivered_2"),
    REACTIVE_ELECTRICITY_RETURNED_2("dsmr/reading/reactive_electricity_returned_2"),
    ELECTRICITY_CURRENTLY_DELIVERED("dsmr/reading/electricity_currently_delivered"),
    ELECTRICITY_CURRENTLY_RETURNED("dsmr/reading/electricity_currently_returned"),
    PHASE_CURRENTLY_DELIVERED_L1("dsmr/reading/phase_currently_delivered_l1"),
    PHASE_CURRENTLY_DELIVERED_L2("dsmr/reading/phase_currently_delivered_l2"),
    PHASE_CURRENTLY_DELIVERED_L3("dsmr/reading/phase_currently_delivered_l3"),
    PHASE_CURRENTLY_RETURNED_L1("dsmr/reading/phase_currently_returned_l1"),
    PHASE_CURRENTLY_RETURNED_L2("dsmr/reading/phase_currently_returned_l2"),
    PHASE_CURRENTLY_RETURNED_L3("dsmr/reading/phase_currently_returned_l3"),
    PHASE_VOLTAGE_L1("dsmr/reading/phase_voltage_l1"),
    PHASE_VOLTAGE_L2("dsmr/reading/phase_voltage_l2"),
    PHASE_VOLTAGE_L3("dsmr/reading/phase_voltage_l3"),
    PHASE_POWER_CURRENT_L1("dsmr/reading/phase_power_current_l1"),
    PHASE_POWER_CURRENT_L2("dsmr/reading/phase_power_current_l2"),
    PHASE_POWER_CURRENT_L3("dsmr/reading/phase_power_current_l3"),
    NOT_EXPECTED("not_expected"),;

    private final String topic;

    SmartGatewaysTopic(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }

    public boolean isExpected() {
        return this != NOT_EXPECTED;
    }

    public static SmartGatewaysTopic from(String subscribeTopic, String topicPrefix) {
        for (SmartGatewaysTopic t : SmartGatewaysTopic.values()) {
            if (subscribeTopic.equals(topicPrefix + "/" + t.topic())) {
                return t;
            }
        }
        return NOT_EXPECTED;
    }
}