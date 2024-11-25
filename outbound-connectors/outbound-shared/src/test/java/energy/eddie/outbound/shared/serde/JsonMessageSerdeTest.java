package energy.eddie.outbound.shared.serde;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.shared.testing.MockDataSourceInformation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMessageSerdeTest {

    //language=JSON
    private static final String JSON = """
            {
                "connectionId": "cid",
                "permissionId": "pid",
                "dataNeedId": "dnid",
                "dataSourceInformation": {
                    "countryCode": "AT",
                    "meteredDataAdministratorId": "mdaid",
                    "permissionAdministratorId": "paid",
                    "regionConnectorId": "at-eda"
                },
                "timestamp": "2024-01-01T00:00:00Z",
                "status": "ACCEPTED",
                "message": "",
                "additionalInformation": null
            }
            """
            .replace("\n", "")
            .replace(" ", "");
    private final ConnectionStatusMessage message = new ConnectionStatusMessage("cid",
                                                                                "pid",
                                                                                "dnid",
                                                                                new MockDataSourceInformation("AT",
                                                                                                              "at-eda",
                                                                                                              "paid",
                                                                                                              "mdaid"),
                                                                                ZonedDateTime.of(2024,
                                                                                                 1,
                                                                                                 1,
                                                                                                 0,
                                                                                                 0,
                                                                                                 0,
                                                                                                 0,
                                                                                                 ZoneOffset.UTC),
                                                                                PermissionProcessStatus.ACCEPTED,
                                                                                "",
                                                                                null
    );

    @Test
    void testSerialize_serializesMessage() throws SerializationException {
        // Given
        var serde = new JsonMessageSerde();

        // When
        var res = serde.serialize(message);

        // Then
        assertEquals(JSON, new String(res, StandardCharsets.UTF_8));
    }

    @Test
    void testSerialize_throwsOnEmptyObject() {
        // Given
        var serde = new JsonMessageSerde();
        var obj = new Object();

        // When & Then
        assertThrows(SerializationException.class, () -> serde.serialize(obj));
    }

    @Test
    void testDeserialize_deserializesMessage() throws DeserializationException {
        // Given
        var serde = new JsonMessageSerde();

        // When
        var res = serde.deserialize("""
                                            {
                                            "key": "value"
                                            }
                                            """.getBytes(StandardCharsets.UTF_8), Map.class);

        // Then
        assertEquals(Map.of("key", "value"), res);
    }

    @Test
    void testDeserialize_throwsOnInvalidJson() {
        // Given
        var serde = new JsonMessageSerde();

        // When
        // Then
        assertThrows(DeserializationException.class,
                     () -> serde.deserialize("jfjklasfjklsa".getBytes(StandardCharsets.UTF_8), Map.class));
    }
}