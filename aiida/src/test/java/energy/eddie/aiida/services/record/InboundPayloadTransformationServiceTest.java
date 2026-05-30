// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record;

import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.record.transform.CimPassThroughTransformer;
import energy.eddie.aiida.services.record.transform.InboundPayloadTransformationService;
import energy.eddie.aiida.services.record.transform.MinMaxEnvelopeOpenAdr3Transformer;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InboundPayloadTransformationServiceTest {
    private final InboundPayloadTransformationService inboundPayloadTransformationService =
            new InboundPayloadTransformationService(
                    ObjectMapperCreatorUtil.mapper(),
                    List.of(new CimPassThroughTransformer(),
                            new MinMaxEnvelopeOpenAdr3Transformer())
            );

    @Test
    void transform_shouldReturnPayloadUnchangedForCim1_12() throws UnsupportedInboundRecordTransformationException {
        var dataSource = mock(InboundDataSource.class);
        var inboundRecord = new InboundRecord(
                Instant.parse("2024-01-15T10:30:00Z"),
                dataSource,
                AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                "{\"message\":\"raw\"}"
        );

        var transformedPayload = inboundPayloadTransformationService.transform(inboundRecord,
                                                                               InboundMessageFormat.CIM_1_12);

        assertEquals("{\"message\":\"raw\"}", transformedPayload);
    }

    @Test
    void transform_shouldThrowForUnsupportedOpenAdrTransformation() {
        var dataSource = mock(InboundDataSource.class);
        when(dataSource.id()).thenReturn(UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606"));

        var inboundRecord = new InboundRecord(
                Instant.parse("2024-01-15T10:30:00Z"),
                dataSource,
                AiidaSchema.SMART_METER_P1_RAW,
                "{\"message\":\"raw\"}"
        );

        var exception = assertThrows(UnsupportedInboundRecordTransformationException.class, () ->
                inboundPayloadTransformationService.transform(inboundRecord, InboundMessageFormat.OPENADR_3)
        );

        assertEquals(
                "Inbound record transformation is not supported for source schema SMART_METER_P1_RAW and target format OPENADR_3.",
                exception.getMessage()
        );
    }

    @Test
    void transform_shouldMapMinMaxEnvelopeToOpenAdr3Event() throws IOException, UnsupportedInboundRecordTransformationException {
        var mapper = ObjectMapperCreatorUtil.mapper();
        var dataSource = mock(InboundDataSource.class);
        var payload = readResource("record/transform/min-max-envelope-openadr3-input.json");
        var inboundRecord = new InboundRecord(
                Instant.parse("2024-01-15T10:30:00Z"),
                dataSource,
                AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                payload
        );

        var transformed = inboundPayloadTransformationService.transform(inboundRecord, InboundMessageFormat.OPENADR_3);
        var expected = readResource("record/transform/min-max-envelope-openadr3-expected.json");

        assertEquals(mapper.readTree(expected), mapper.readTree(transformed));
    }

    private String readResource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
