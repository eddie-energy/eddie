// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;

@FunctionalInterface
public interface ModbusResponseDataExtractor<T> {
    T extractAndProcess(ModbusResponse response, ModbusDataPoint dataPoint) throws ModbusException;
}
