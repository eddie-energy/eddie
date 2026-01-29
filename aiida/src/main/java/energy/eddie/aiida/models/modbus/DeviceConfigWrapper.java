// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.modbus;

import jakarta.annotation.Nullable;

import java.util.List;

public record DeviceConfigWrapper(@Nullable ModbusWrapper modbus) {

    public List<ModbusDevice> devices() {
        return modbus != null ? modbus.getDevices() : List.of();
    }

    public record ModbusWrapper(@Nullable List<ModbusDevice> devices) {
        public List<ModbusDevice> getDevices() {
            return devices != null ? devices : List.of();
        }
    }
}

