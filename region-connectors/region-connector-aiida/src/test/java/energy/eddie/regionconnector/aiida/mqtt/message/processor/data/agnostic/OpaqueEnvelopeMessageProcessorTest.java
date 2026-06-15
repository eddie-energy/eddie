// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.mqtt.message.processor.data.agnostic;

import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpaqueEnvelopeMessageProcessorTest {
    private static final String PERMISSION_ID = "perm-id";

    private final Sinks.Many<OpaqueEnvelope> opaqueEnvelopeSink = Sinks.many().unicast().onBackpressureBuffer();
    private OpaqueEnvelopeMessageProcessor processor;

    @Mock
    private AiidaPermissionRequestViewRepository permissionRequestViewRepository;
    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        processor = new OpaqueEnvelopeMessageProcessor(permissionRequestViewRepository,
                                                       objectMapper,
                                                       opaqueEnvelopeSink);
    }

    @Test
    void processMessage_emitsOpaqueEnvelopeForValidPermission() {
        // Given
        var envelope = new OpaqueEnvelope("rc-id",
                                          PERMISSION_ID,
                                          "connection-id",
                                          "data-need-id",
                                          "message-id",
                                          ZonedDateTime.parse("2026-06-23T00:00:00Z"),
                                          "payload");
        var permission = acceptedPermission();
        when(objectMapper.readValue(any(byte[].class), eq(OpaqueEnvelope.class))).thenReturn(envelope);
        when(permissionRequestViewRepository.findByPermissionId(PERMISSION_ID)).thenReturn(Optional.of(permission));
        var message = new MqttMessage("{}".getBytes(UTF_8));

        // When / Then
        StepVerifier.create(opaqueEnvelopeSink.asFlux())
                    .then(() -> {
                        try {
                            processor.processMessage(message);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .assertNext(emitted -> assertEquals(envelope, emitted))
                    .thenCancel()
                    .verify();

        verify(permissionRequestViewRepository).findByPermissionId(PERMISSION_ID);
        verify(objectMapper).readValue(message.getPayload(), OpaqueEnvelope.class);
    }

    @Test
    void forTopicPath_returnsOpaqueOutboundTopic() {
        assertEquals("data/outbound/opaque", processor.forTopicPath());
    }

    private AiidaPermissionRequest acceptedPermission() {
        var start = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        var end = LocalDate.now(ZoneId.systemDefault()).plusDays(1);

        var permission = mock(AiidaPermissionRequest.class);
        when(permission.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        when(permission.start()).thenReturn(start);
        when(permission.end()).thenReturn(end);
        return permission;
    }
}
