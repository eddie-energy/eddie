// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record;

import com.jayway.jsonpath.JsonPath;
import energy.eddie.aiida.ObjectMapperCreatorUtil;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.record.transform.CimPassThroughTransformer;
import energy.eddie.aiida.services.record.transform.InboundPayloadTransformationService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.recmmoe.*;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
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
                    List.of(new CimPassThroughTransformer())
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
                AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                "{\"message\":\"raw\"}"
        );

        var exception = assertThrows(UnsupportedInboundRecordTransformationException.class, () ->
                inboundPayloadTransformationService.transform(inboundRecord, InboundMessageFormat.OPENADR_3)
        );

        assertEquals(
                "Inbound record transformation is not supported for source schema MIN_MAX_ENVELOPE_CIM_V1_12 and target format OPENADR_3.",
                exception.getMessage()
        );
    }
}
