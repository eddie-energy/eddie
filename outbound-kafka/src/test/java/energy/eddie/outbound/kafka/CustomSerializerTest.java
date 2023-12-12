package energy.eddie.outbound.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomSerializerTest {

    private CustomSerializer customSerializer;

    @BeforeEach
    void setup() {
        customSerializer = new CustomSerializer();
    }

    @AfterEach
    void tearDown() {
        customSerializer.close();
    }

    @Test
    void testSerialize_StatusMessageData() {
        String topic = "test";
        ZonedDateTime now = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ConnectionStatusMessage data = new ConnectionStatusMessage("connectionId", "permissionId", "dataNeedId", new TestDataSourceInformation("cc", "rc", "pa", "mda"), now, PermissionProcessStatus.ACCEPTED, "Granted");
        byte[] expected = "{\"connectionId\":\"connectionId\",\"permissionId\":\"permissionId\",\"dataNeedId\":\"dataNeedId\",\"dataSourceInformation\":{\"countryCode\":\"cc\",\"meteredDataAdministratorId\":\"mda\",\"permissionAdministratorId\":\"pa\",\"regionConnectorId\":\"rc\"},\"timestamp\":1672531200.000000000,\"status\":\"ACCEPTED\",\"message\":\"Granted\"}"
                .getBytes(StandardCharsets.UTF_8);

        byte[] result = customSerializer.serialize(topic, data);
        assertArrayEquals(expected, result);
    }

    @Test
    void testSerialize_ConsumptionRecordData() throws JsonProcessingException {
        String topic = "test";
        ConsumptionRecord data = new ConsumptionRecord();
        byte[] expected = new ObjectMapper().writeValueAsBytes(data);

        byte[] result = customSerializer.serialize(topic, data);

        assertArrayEquals(expected, result);
    }

    @Test
    void testSerialize_NullData() {
        String topic = "test";
        Object data = null;
        byte[] expected = new byte[0];

        byte[] result = customSerializer.serialize(topic, data);

        assertArrayEquals(expected, result);
    }

    @Test
    void testSerialize_UnsupportedDataType() {
        String topic = "test";
        Object data = new Object();

        assertThrows(UnsupportedOperationException.class,
                () -> customSerializer.serialize(topic, data));
    }

    @Test
    void testSerialize_EddieValidatedHistoricalDataMarketDocument() throws JsonProcessingException {
        String topic = "test";
        EddieValidatedHistoricalDataMarketDocument data = new EddieValidatedHistoricalDataMarketDocument(
                Optional.of("connectionId"),
                Optional.of("permissionId"),
                Optional.of("dataNeedId"),
                new energy.eddie.cim.validated_historical_data.v0_82.ValidatedHistoricalDataMarketDocument()
        );
        byte[] expected = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsBytes(data);

        byte[] result = customSerializer.serialize(topic, data);

        assertArrayEquals(expected, result);
    }

    private record TestDataSourceInformation(String countryCode,
                                             String regionConnectorId,
                                             String permissionAdministratorId,
                                             String meteredDataAdministratorId) implements DataSourceInformation {
    }
}