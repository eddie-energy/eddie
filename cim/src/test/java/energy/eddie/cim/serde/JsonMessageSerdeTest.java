// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.agnostic.SimpleDataSourceInformation;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.NullNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMessageSerdeTest {
    @Test
    void testSerialize_serializesMessage() throws SerializationException {
        // Given
        var serde = new JsonMessageSerde();
        var message = Map.of("key", "value");
        //language=json
        var expected = "{\"key\":\"value\"}";

        // When
        var res = serde.serialize(message);

        // Then
        assertEquals(expected, new String(res, StandardCharsets.UTF_8));
    }

    @Test
    void testDeserialize_deserializesMessage() throws DeserializationException {
        // Given
        var serde = new JsonMessageSerde();
        //language=JSON
        var message = "{\"key\":\"value\"}";

        // When
        var res = serde.deserialize(message.getBytes(StandardCharsets.UTF_8), Map.class);

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

    @SuppressWarnings({"resource", "DataFlowIssue"})
    @Test
    void testDeserializeList_forNonCimType() throws IOException, DeserializationException {
        // Given
        var expected = List.of(
                new ConnectionStatusMessage(
                        "1",
                        "5c945d84-8120-4f30-9c69-2dc1dbbb4a43",
                        "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                        new SimpleDataSourceInformation("DE", "sim", "sim", "sim"),
                        ZonedDateTime.parse("2026-07-13T00:00:00.000000000Z"),
                        PermissionProcessStatus.CREATED,
                        "",
                        NullNode.getInstance()
                ),
                new ConnectionStatusMessage(
                        "1",
                        "5c945d84-8120-4f30-9c69-2dc1dbbb4a43",
                        "9bd0668f-cc19-40a8-99db-dc2cb2802b17",
                        new SimpleDataSourceInformation("DE", "sim", "sim", "sim"),
                        ZonedDateTime.parse("2026-07-13T00:00:00.000000000Z"),
                        PermissionProcessStatus.VALIDATED,
                        "",
                        NullNode.getInstance()
                )
        );
        var serde = new JsonMessageSerde();

        var input = JsonMessageSerde.class
                .getResourceAsStream("/agnostic/connectionStatusMessageList.json")
                .readAllBytes();

        // When
        var res = serde.deserializeList(input, ConnectionStatusMessage.class);

        // Then
        assertEquals(expected, res);
    }

    @SuppressWarnings({"resource", "DataFlowIssue"})
    @Test
    void testDeserializeList_forCimType() throws IOException, DeserializationException {
        // Given
        var input = JsonMessageSerde.class
                .getResourceAsStream("/cim/v0_82/permissionMarketDocumentList.json")
                .readAllBytes();
        var serde = new JsonMessageSerde();

        // When
        var res = serde.deserializeList(input, PermissionEnvelope.class);

        // Then
        assertEquals(2, res.size());
    }
}