// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.modbus;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import energy.eddie.aiida.models.modbus.Endian;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ModbusTcpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModbusTcpClient.class);
    private static final int DEFAULT_ERROR_MESSAGE_COUNT = 0;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final TCPMasterConnection connection;
    private final int unitId;
    @Nullable
    private ScheduledFuture<?> reconnectFuture;
    private int messageErrorCounter;

    public ModbusTcpClient(String host, int port, int unitId) throws Exception {
        this.unitId = unitId;
        this.messageErrorCounter = DEFAULT_ERROR_MESSAGE_COUNT;
        this.connection = createConnection(host, port);

        LOGGER.info("Connected to Modbus TCP device at {}:{} with unitId {}", host, port, unitId);
    }

    protected ModbusTcpClient(TCPMasterConnection connection, int unitId) {
        this.connection = connection;
        this.unitId = unitId;
        this.messageErrorCounter = DEFAULT_ERROR_MESSAGE_COUNT;
    }

    public Optional<Object> readHoldingRegister(ModbusDataPoint modbusDataPoint) {
        return readModbusData(
                modbusDataPoint,
                ReadMultipleRegistersRequest::new,
                (response, dataPoint) -> {
                    if (response instanceof ReadMultipleRegistersResponse rmrResponse) {
                        return parseRegisterResponse(rmrResponse.getRegisters(), dataPoint)
                                .orElseThrow(() -> new ModbusException("Failed to parse register response data."));
                    }
                    throw new ModbusException("Not a ReadMultipleRegistersResponse.");
                }
        );
    }

    public Optional<Object> readInputRegister(ModbusDataPoint modbusDataPoint) {
        return readModbusData(
                modbusDataPoint,
                ReadInputRegistersRequest::new,
                (response, dataPoint) -> {
                    if (response instanceof ReadInputRegistersResponse rirResponse) {
                        return parseRegisterResponse(rirResponse.getRegisters(), dataPoint)
                                .orElseThrow(() -> new ModbusException("Failed to parse register response data."));
                    }
                    throw new ModbusException("Not a ReadInputRegistersResponse.");
                }
        );
    }

    public Optional<Object> readCoil(ModbusDataPoint modbusDataPoint) {
        return readModbusData(
                modbusDataPoint,
                ReadCoilsRequest::new,
                (response, ignored) -> {
                    if (response instanceof ReadCoilsResponse coilsResponse) {
                        return coilsResponse.getCoilStatus(0);
                    }
                    throw new ModbusException("Not a ReadCoilsResponse.");
                }
        );
    }

    public Optional<Object> readDiscreteInput(ModbusDataPoint modbusDataPoint) {
        return readModbusData(
                modbusDataPoint,
                ReadInputDiscretesRequest::new,
                (response, ignored) -> {
                    if (response instanceof ReadInputDiscretesResponse discreteResponse) {
                        return discreteResponse.getDiscreteStatus(0);
                    }
                    throw new ModbusException("Not a ReadInputDiscretesResponse.");
                }
        );
    }

    public void close() {
        if (connection != null && connection.isConnected()) {
            connection.close();
            LOGGER.info("Modbus connection to device {}:{} closed.", connection.getAddress(), connection.getPort());
        }

        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            reconnectFuture.cancel(true);
            LOGGER.info("Cancelled pending reconnect task.");
        }

        scheduler.shutdownNow();
    }

    public void reconnect() {
        if (connection == null) {
            LOGGER.warn("Connection object is null; cannot reconnect.");
            return;
        }

        if (connection.isConnected()) {
            connection.close();
            LOGGER.info("Closed existing Modbus connection to device {}:{} before reconnecting.",
                        connection.getAddress(),
                        connection.getPort());
        }

        try {
            connection.connect();
            LOGGER.info("Modbus device {}:{} reconnected.", connection.getAddress(), connection.getPort());
        } catch (Exception e) {
            LOGGER.error("Failed to reconnect to Modbus device {}:{} -> try again in 30 seconds",
                         connection.getAddress(),
                         connection.getPort(),
                         e);
            this.reconnectFuture = scheduler.schedule(this::reconnect, 30, TimeUnit.SECONDS);
        }
    }

    protected TCPMasterConnection createConnection(String host, int port) throws Exception {
        var addr = InetAddress.getByName(host);
        var conn = new TCPMasterConnection(addr);

        conn.setPort(port);
        conn.connect();

        return conn;
    }

    private <T> Optional<Object> readModbusData(
            ModbusDataPoint modbusDataPoint,
            ModbusRequestCreator modbusRequestCreator,
            ModbusResponseDataExtractor<T> modbusResponseDataExtractor
    ) {
        try {
            var request = modbusRequestCreator.create(modbusDataPoint.register(), modbusDataPoint.length());
            request.setUnitID(unitId);

            var transaction = new ModbusTCPTransaction(connection);
            transaction.setRequest(request);
            transaction.execute();

            var response = transaction.getResponse();
            T result = modbusResponseDataExtractor.extractAndProcess(response, modbusDataPoint);

            return Optional.of(result);
        } catch (Exception e) {
            return handleError(modbusDataPoint, e);
        }
    }

    private Optional<Object> parseRegisterResponse(InputRegister[] inputRegisters, ModbusDataPoint modbusDataPoint) {
        var buffer = ByteBuffer.allocate(modbusDataPoint.length() * 2);
        buffer.order(modbusDataPoint.endian() == Endian.LITTLE ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        for (InputRegister inputRegister : inputRegisters) {
            buffer.putShort((short) inputRegister.getValue());
        }

        buffer.rewind();
        return Optional.of(switch (modbusDataPoint.valueType()) {
            case "int16" -> buffer.getShort();
            case "uint16" -> buffer.getShort() & 0xFFFF;
            case "int32" -> buffer.getInt();
            case "uint32" -> buffer.getInt() & 0xFFFFFFFFL;
            case "float32" -> buffer.getFloat();
            case "string" -> {
                var stringBytes = new byte[modbusDataPoint.length() * 2];
                buffer.get(stringBytes);

                int nullTerminator = 0;
                while (nullTerminator < stringBytes.length && stringBytes[nullTerminator] != 0) {
                    nullTerminator++;
                }

                yield new String(stringBytes, 0, nullTerminator, StandardCharsets.UTF_8);
            }
            default -> throw new IllegalArgumentException("Unsupported type: " + modbusDataPoint.valueType());
        });
    }

    private Optional<Object> handleError(ModbusDataPoint dp, Exception e) {
        messageErrorCounter++;
        LOGGER.warn("Failed to read register {}: {}", dp.register(), e.getMessage());

        if (messageErrorCounter > 15) {
            messageErrorCounter = 0;
            LOGGER.warn("Error counter exceeded - reconnecting to Modbus device {}:{}",
                        connection.getAddress(),
                        connection.getPort());
            reconnect();
        }

        return Optional.empty();
    }
}
