package energy.eddie.aiida.adapters.datasource.sga;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmartGatewaysAdapterMessageTest {
    @Test
    void verify_isProperlyDeserialized() {
        var message = """
                4530303632303030303037393239333232
                4730303732303034303036353733323230
                0002
                3845.467
                2621.303
                1894.882
                1435.228
                0.000
                0.000
                0.000
                0.000
                0.531
                0.030
                0
                71
                460
                30
                0
                0
                233.00
                234.00
                234.00
                0.00
                0.00
                2.00
                736.650
                -0.061
                0.002""";

        var actual = SmartGatewaysAdapterValueDeserializer.deserialize(message.getBytes(StandardCharsets.UTF_8));

        assertEquals("0002", actual.electricityTariff().value());
        assertEquals("3845.467", actual.electricityDeliveredTariff1().value());
        assertEquals("1435.228", actual.electricityReturnedTariff2().value());
        assertEquals("0.531", actual.powerCurrentlyDelivered().value());
        assertEquals("736.650", actual.gasDelivered().value());
        assertEquals("460", actual.phaseCurrentlyDeliveredL3().value());
    }
}
