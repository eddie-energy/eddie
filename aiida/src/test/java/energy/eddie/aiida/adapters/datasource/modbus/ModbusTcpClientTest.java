package energy.eddie.aiida.adapters.datasource.modbus;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import energy.eddie.aiida.models.modbus.Endian;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;
import energy.eddie.aiida.models.modbus.RegisterType;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModbusTcpClientTest {

    private ModbusTcpClient client;
    private TCPMasterConnection mockConnection;

    @BeforeEach
    void setUp() {
        mockConnection = mock(TCPMasterConnection.class);
        client = new ModbusTcpClient(mockConnection, 1);
    }

    @Test
    void testReadHoldingRegister_uint16_littleEndian() {
        ModbusDataPoint dp = new ModbusDataPoint("status", 10, RegisterType.HOLDING, 1, "uint16", Endian.LITTLE, false, null, null, null, null);

        ReadMultipleRegistersResponse response = mock(ReadMultipleRegistersResponse.class);
        Register[] regs = new Register[]{new SimpleRegister(0x01F4)};
        when(response.getRegisters()).thenReturn(regs);

        try (MockedConstruction<ModbusTCPTransaction> mocked = Mockito.mockConstruction(ModbusTCPTransaction.class,
                (mock, context) -> {
                    when(mock.getResponse()).thenReturn(response);
                    doNothing().when(mock).setRequest(any());
                    doNothing().when(mock).execute();
                })) {
            Optional<Object> result = client.readHoldingRegister(dp);
            assertTrue(result.isPresent());
            assertEquals(500, result.get());
        }
    }

    @Test
    void testReadInputRegister_float32_littleEndian() {
        ModbusDataPoint dp = new ModbusDataPoint("soc", 20, RegisterType.INPUT, 2, "float32", Endian.LITTLE, false, null, null, null, null);

        float expected = 42.42f;
        int bits = Float.floatToIntBits(expected);
        short lo = (short) (bits & 0xFFFF);
        short hi = (short) ((bits >> 16) & 0xFFFF);

        Register[] regs = new Register[]{
                new SimpleRegister(lo),
                new SimpleRegister(hi)
        };

        ReadInputRegistersResponse response = mock(ReadInputRegistersResponse.class);
        when(response.getRegisters()).thenReturn(regs);

        try (MockedConstruction<ModbusTCPTransaction> mocked = Mockito.mockConstruction(ModbusTCPTransaction.class,
                (mock, context) -> {
                    when(mock.getResponse()).thenReturn(response);
                    doNothing().when(mock).setRequest(any());
                    doNothing().when(mock).execute();
                })) {

            Optional<Object> result = client.readInputRegister(dp);
            assertTrue(result.isPresent());
            assertEquals(expected, (Float) result.get(), 0.0001);
        }
    }

    @Test
    void testReadCoil_true() {
        ModbusDataPoint dp = new ModbusDataPoint("coil_on", 1, RegisterType.COIL, 1, "bool", Endian.LITTLE, false, null, null, null, null);

        ReadCoilsResponse response = mock(ReadCoilsResponse.class);
        when(response.getCoilStatus(0)).thenReturn(true);

        try (MockedConstruction<ModbusTCPTransaction> mocked = Mockito.mockConstruction(ModbusTCPTransaction.class,
                (mock, context) -> {
                    when(mock.getResponse()).thenReturn(response);
                    doNothing().when(mock).setRequest(any());
                    doNothing().when(mock).execute();
                })) {
            Optional<Object> result = client.readCoil(dp);
            assertTrue(result.isPresent());
            assertEquals(true, result.get());
        }
    }

    @Test
    void testReadDiscreteInput_false() {
        ModbusDataPoint dp = new ModbusDataPoint("input_off", 2, RegisterType.DISCRETE, 1, "bool", Endian.LITTLE, false, null, null, null, null);

        ReadInputDiscretesResponse response = mock(ReadInputDiscretesResponse.class);
        when(response.getDiscreteStatus(0)).thenReturn(false);

        try (MockedConstruction<ModbusTCPTransaction> mocked = Mockito.mockConstruction(ModbusTCPTransaction.class,
                (mock, context) -> {
                    when(mock.getResponse()).thenReturn(response);
                    doNothing().when(mock).setRequest(any());
                    doNothing().when(mock).execute();
                })) {
            Optional<Object> result = client.readDiscreteInput(dp);
            assertTrue(result.isPresent());
            assertEquals(false, result.get());
        }
    }

    @Test
    void testClose() {
        when(mockConnection.isConnected()).thenReturn(true);
        client.close();
        verify(mockConnection).close();
    }

    @Test
    void testReconnect() throws Exception {
        when(mockConnection.isConnected()).thenReturn(true, false);
        client.reconnect();
        verify(mockConnection).close();
        verify(mockConnection).connect();
    }

    @Test
    void testHandleErrorAndReconnectAfterThreshold() throws Exception {
        when(mockConnection.getAddress()).thenReturn(null);
        when(mockConnection.getPort()).thenReturn(502);
        for (int i = 0; i < 16; i++) {
            client.readHoldingRegister(new ModbusDataPoint("test", 0, RegisterType.HOLDING, 1, "uint16", Endian.LITTLE, false, null, null, null, null));
        }
        // Reconnect should be triggered once after threshold
        verify(mockConnection, atLeastOnce()).connect();
    }

    @Test
    void testParse_uint16() {
        ModbusDataPoint dp = new ModbusDataPoint("val", 0, RegisterType.HOLDING, 1, "uint16", Endian.BIG, false, null, null, null, null);
        Optional<Object> result = client.readHoldingRegister(dp);
        assertTrue(result.isEmpty()); // because transaction is not mocked
    }

    @Test
    void testParse_int16() {
        InputRegister[] regs = {new SimpleInputRegister((short) -1234)};
        ModbusDataPoint dp = new ModbusDataPoint("val", 0, RegisterType.HOLDING, 1, "int16", Endian.BIG, false, null, null, null, null);
        Optional<Object> val = callParseRegisterResponse(regs, dp);
        assertEquals((short) -1234, val.orElse(0));
    }

    @Test
    void testParse_int32() {
        int value = 0x12345678;
        InputRegister[] regs = {
                new SimpleInputRegister((short) (value >> 16)),
                new SimpleInputRegister((short) (value & 0xFFFF))
        };
        ModbusDataPoint dp = new ModbusDataPoint("val", 0, RegisterType.HOLDING, 2, "int32", Endian.BIG, false, null, null, null, null);
        Optional<Object> result = callParseRegisterResponse(regs, dp);
        assertEquals(value, result.orElse(0));
    }

    @Test
    void testParse_float32() {
        float f = 3.14f;
        int bits = Float.floatToIntBits(f);
        InputRegister[] regs = {
                new SimpleInputRegister((short) (bits & 0xFFFF)),
                new SimpleInputRegister((short) ((bits >> 16) & 0xFFFF))
        };
        ModbusDataPoint dp = new ModbusDataPoint("val", 0, RegisterType.HOLDING, 2, "float32", Endian.LITTLE, false, null, null, null, null);
        Optional<Object> result = callParseRegisterResponse(regs, dp);
        assert result.orElse(0.0) instanceof Float;
        assertEquals(f, (Float) result.orElse(0.0), 0.0001);
    }

    @Test
    void testParse_string() {
        String original = "test";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        InputRegister[] regs = new InputRegister[bytes.length / 2 + 1];
        for (int i = 0; i < regs.length; i++) {
            short hi = (i * 2 + 1 < bytes.length) ? (short) (bytes[i * 2 + 1] & 0xFF) : 0;
            short lo = (i * 2 < bytes.length) ? (short) (bytes[i * 2] & 0xFF) : 0;
            regs[i] = new SimpleInputRegister((short) ((hi << 8) | lo));
        }
        ModbusDataPoint dp = new ModbusDataPoint("val", 0, RegisterType.HOLDING, regs.length, "string", Endian.LITTLE, false, null, null, null, null);
        Optional<Object> result = callParseRegisterResponse(regs, dp);
        assertEquals("test", result.orElse(0));
    }

    private Optional<Object> callParseRegisterResponse(InputRegister[] regs, ModbusDataPoint dp) {
        try {
            var method = ModbusTcpClient.class.getDeclaredMethod("parseRegisterResponse", InputRegister[].class, ModbusDataPoint.class);
            method.setAccessible(true);
            return (Optional<Object>) method.invoke(client, regs, dp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        client.close();
    }
}
