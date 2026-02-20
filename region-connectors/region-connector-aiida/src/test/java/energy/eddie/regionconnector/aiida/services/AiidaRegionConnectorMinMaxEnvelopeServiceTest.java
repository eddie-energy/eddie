// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.recmmoe.MessageDocumentHeader;
import energy.eddie.cim.v1_12.recmmoe.MetaInformation;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorMinMaxEnvelopeServiceTest {
    @Mock
    private MqttService mqttService;

    @InjectMocks
    private AiidaRegionConnectorMinMaxEnvelopeService service;

    @Test
    void minMaxEnvelopeArrived_publishesToMqtt() throws Exception {
        // Given
        var permissionId = "test-permission-id";
        var metaInformation = new MetaInformation()
                .withRequestPermissionId(permissionId);
        var header = new MessageDocumentHeader()
                .withMetaInformation(metaInformation);
        var envelope = new RECMMOEEnvelope()
                .withMessageDocumentHeader(header);

        // When
        service.minMaxEnvelopeArrived(envelope);

        // Then
        verify(mqttService).publishInboundData(AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12, permissionId, envelope);
    }
}
