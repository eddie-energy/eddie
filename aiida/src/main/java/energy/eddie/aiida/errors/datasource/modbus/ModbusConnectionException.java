// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource.modbus;

public class ModbusConnectionException extends RuntimeException {
    public ModbusConnectionException(String message, Exception cause) {
        super(message, cause);
    }
}
