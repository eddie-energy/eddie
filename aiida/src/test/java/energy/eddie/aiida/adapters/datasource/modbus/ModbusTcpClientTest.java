package energy.eddie.aiida.adapters.datasource.modbus;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import energy.eddie.aiida.models.modbus.Endian;
import energy.eddie.aiida.models.modbus.ModbusDataPoint;
import energy.eddie.aiida.models.modbus.RegisterType;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModbusTcpClientTest {

    private ModbusTcpClient client;

    static class TestableModbusTcpClient extends ModbusTcpClient {
        private final TCPMasterConnection injectedConnection;

        public TestableModbusTcpClient(TCPMasterConnection injectedConnection, int unitId) throws Exception {
            super("127.0.0.1", 502, unitId);
            this.injectedConnection = injectedConnection;
        }

        @Override
        protected TCPMasterConnection createConnection(String host, int port) {
            return injectedConnection;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        TCPMasterConnection mockConnection = mock(TCPMasterConnection.class);
        client = new TestableModbusTcpClient(mockConnection, 1);
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

    @AfterEach
    void tearDown() {
        client.close();
    }
}
