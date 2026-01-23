// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource.modbus;

public class ModbusDeviceConfigException extends RuntimeException {
    public ModbusDeviceConfigException(String message) {
        super(message);
    }

    public ModbusDeviceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
