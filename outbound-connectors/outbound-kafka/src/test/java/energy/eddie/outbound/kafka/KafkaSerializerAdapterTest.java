// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.kafka;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.MessageSerde;
import energy.eddie.cim.serde.SerdeFactory;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.shared.testing.MockDataSourceInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaSerializerAdapterTest {
    @Mock
    private MessageSerde mockSerde;

    @Test
    void testSerialize_StatusMessageData() throws SerdeInitializationException {
        var customSerializer = new KafkaSerializerAdapter(SerdeFactory.getInstance().create("json"));
        String topic = "test";
        ZonedDateTime now = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ConnectionStatusMessage data = new ConnectionStatusMessage("connectionId",
                                                                   "permissionId",
                                                                   "dataNeedId",
                                                                   new MockDataSourceInformation("cc",
                                                                                                 "rc",
                                                                                                 "pa",
                                                                                                 "mda"),
                                                                   now,
                                                                   PermissionProcessStatus.ACCEPTED,
                                                                   "Granted",
                                                                   new ObjectMapper().createObjectNode()
                                                                                     .put("test", "value"));
        byte[] expected = "{\"connectionId\":\"connectionId\",\"permissionId\":\"permissionId\",\"dataNeedId\":\"dataNeedId\",\"dataSourceInformation\":{\"countryCode\":\"cc\",\"meteredDataAdministratorId\":\"mda\",\"permissionAdministratorId\":\"pa\",\"regionConnectorId\":\"rc\"},\"timestamp\":\"2023-01-01T00:00:00Z\",\"status\":\"ACCEPTED\",\"message\":\"Granted\",\"additionalInformation\":{\"test\":\"value\"}}"
                .getBytes(StandardCharsets.UTF_8);

        byte[] result = customSerializer.serialize(topic, data);
        assertArrayEquals(expected, result);

        customSerializer.close();
    }

    @Test
    void testSerialize_NullData() throws SerdeInitializationException {
        var customSerializer = new KafkaSerializerAdapter(SerdeFactory.getInstance().create("json"));
        String topic = "test";

        byte[] result = customSerializer.serialize(topic, null);

        assertNull(result);

        customSerializer.close();
    }

    @Test
    void testSerialize_throwsOnSerializationException() throws SerializationException {
        // Given
        var customSerializer = new KafkaSerializerAdapter(mockSerde);
        when(mockSerde.serialize(any())).thenThrow(new SerializationException(null));

        // When
        var res = customSerializer.serialize("any", new RTREnvelope());

        // Then
        assertNull(res);

        // Clean-Up
        customSerializer.close();
    }
}
