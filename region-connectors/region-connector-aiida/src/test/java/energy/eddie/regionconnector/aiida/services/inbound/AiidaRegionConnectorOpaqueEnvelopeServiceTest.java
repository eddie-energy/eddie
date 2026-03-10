// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services.inbound;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.opaque.OpaqueEnvelope;
import energy.eddie.regionconnector.aiida.services.MqttService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorOpaqueEnvelopeServiceTest {
    @Mock
    private MqttService mqttService;

    @InjectMocks
    private AiidaRegionConnectorOpaqueEnvelopeService service;

    @Test
    void opaqueEnvelopeArrived_publishesToMqtt() throws Exception {
        // Given
        var id = "test-id";
        var envelope = new OpaqueEnvelope(id, id, id, id, id, ZonedDateTime.now(), "test-payload");

        // When
        service.opaqueEnvelopeArrived(envelope);

        // Then
        verify(mqttService).publishInboundData(AiidaSchema.OPAQUE, id, envelope);
    }
}
