// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.modbus;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;

@FunctionalInterface
public interface ModbusRequestCreator {
    ModbusRequest create(int register, int length);
}
