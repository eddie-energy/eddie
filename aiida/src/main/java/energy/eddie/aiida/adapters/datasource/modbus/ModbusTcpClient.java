package energy.eddie.aiida.adapters.datasource.modbus;

import com.ghgande.j2mod.modbus.msg.*;
import energy.eddie.aiida.models.modbus.Endian;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
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

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    @Nullable
    private ScheduledFuture<?> reconnectFuture;
    private final TCPMasterConnection connection;
    private final int unitId;
    private int messageErrorCounter;

    public ModbusTcpClient(String host, int port, int unitId) throws Exception {
        this.unitId = unitId;
        this.messageErrorCounter = 0;
        connection = createConnection(host, port);
        LOGGER.info("Connected to Modbus TCP device at {}:{} with unitId {}", host, port, unitId);
    }

    protected ModbusTcpClient(TCPMasterConnection connection, int unitId) {
        this.connection = connection;
        this.unitId = unitId;
        this.messageErrorCounter = 0;
    }


    protected TCPMasterConnection createConnection(String host, int port) throws Exception {
        InetAddress addr = InetAddress.getByName(host);
        TCPMasterConnection conn = new TCPMasterConnection(addr);
        conn.setPort(port);
        conn.connect();
        return conn;
    }

    public Optional<Object> readHoldingRegister(ModbusDataPoint dp) {
        try {
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            ReadMultipleRegistersRequest request = new ReadMultipleRegistersRequest(dp.register(), dp.getLength());
            request.setUnitID(unitId);
            transaction.setRequest(request);
            transaction.execute();

            ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) transaction.getResponse();
            return parseRegisterResponse(res.getRegisters(), dp);
        } catch (Exception e) {
            return handleError(dp, e);
        }
    }

    public Optional<Object> readInputRegister(ModbusDataPoint dp) {
        try {
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            ReadInputRegistersRequest request = new ReadInputRegistersRequest(dp.register(), dp.getLength());
            request.setUnitID(unitId);
            transaction.setRequest(request);
            transaction.execute();

            ReadInputRegistersResponse res = (ReadInputRegistersResponse) transaction.getResponse();
            return parseRegisterResponse(res.getRegisters(), dp);
        } catch (Exception e) {
            return handleError(dp, e);
        }
    }

    public Optional<Object> readCoil(ModbusDataPoint dp) {
        try {
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            ReadCoilsRequest request = new ReadCoilsRequest(dp.register(), dp.getLength());
            request.setUnitID(unitId);
            transaction.setRequest(request);
            transaction.execute();

            ReadCoilsResponse res = (ReadCoilsResponse) transaction.getResponse();
            return Optional.of(res.getCoilStatus(0));
        } catch (Exception e) {
            return handleError(dp, e);
        }
    }

    public Optional<Object> readDiscreteInput(ModbusDataPoint dp) {
        try {
            ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
            ReadInputDiscretesRequest request = new ReadInputDiscretesRequest(dp.register(), dp.getLength());
            request.setUnitID(unitId);
            transaction.setRequest(request);
            transaction.execute();

            ReadInputDiscretesResponse res = (ReadInputDiscretesResponse) transaction.getResponse();
            return Optional.of(res.getDiscreteStatus(0));
        } catch (Exception e) {
            return handleError(dp, e);
        }
    }

    private Optional<Object> parseRegisterResponse(InputRegister[] regs, ModbusDataPoint dp) {
        ByteBuffer buffer = ByteBuffer.allocate(dp.getLength() * 2);
        buffer.order(dp.endian() == Endian.LITTLE ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        for (InputRegister reg : regs) {
            buffer.putShort((short) reg.getValue());
        }

        buffer.rewind();
        return Optional.of(switch (dp.valueType()) {
            case "int16" -> buffer.getShort();
            case "uint16" -> buffer.getShort() & 0xFFFF;
            case "int32" -> buffer.getInt();
            case "uint32" -> buffer.getInt() & 0xFFFFFFFFL;
            case "float32" -> buffer.getFloat();
            case "string" -> {
                byte[] strBytes = new byte[dp.getLength() * 2];
                buffer.get(strBytes);

                int nullTerminator = 0;
                while (nullTerminator < strBytes.length && strBytes[nullTerminator] != 0) {
                    nullTerminator++;
                }

                yield new String(strBytes, 0, nullTerminator, StandardCharsets.UTF_8);
            }
            default -> throw new IllegalArgumentException("Unsupported type: " + dp.valueType());
        });
    }

    private Optional<Object> handleError(ModbusDataPoint dp, Exception e) {
        messageErrorCounter++;
        LOGGER.warn("Failed to read register {}: {}", dp.register(), e.getMessage());
        if (messageErrorCounter > 15) {
            messageErrorCounter = 0;
            LOGGER.warn("Error counter exceeded - reconnecting to Modbus device {}:{}", connection.getAddress(), connection.getPort());
            reconnect();
        }
        return Optional.empty();
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
        if (connection != null) {
            try {
                if (connection.isConnected()) {
                    connection.close();
                    LOGGER.info("Closed existing Modbus connection to device {}:{} before reconnecting.", connection.getAddress(), connection.getPort());
                }

                connection.connect();
                LOGGER.info("Modbus device {}:{} reconnected.", connection.getAddress(), connection.getPort());
            } catch (Exception e) {
                LOGGER.error("Failed to reconnect to Modbus device {}:{} -> try again in 30 seconds", connection.getAddress(), connection.getPort(), e);
                this.reconnectFuture = scheduler.schedule(this::reconnect, 30, TimeUnit.SECONDS);
            }
        } else {
            LOGGER.warn("Connection object is null; cannot reconnect.");
        }
    }

}
