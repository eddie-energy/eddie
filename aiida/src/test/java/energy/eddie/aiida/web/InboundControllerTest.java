// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.record.InboundRecordDto;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.services.record.InboundRecordService;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InboundController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class InboundControllerTest {
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ACCESS_CODE = "test-access-code";
    private static final InboundRecordDto INBOUND_RECORD_DTO = new InboundRecordDto(
            Instant.parse("2026-01-01T00:00:00Z"),
            USER_ID,
            DATA_SOURCE_ID,
            AiidaAsset.CONNECTION_AGREEMENT_POINT,
            null,
            null,
            AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
            InboundMessageFormat.CIM_1_12,
            "mapped-payload"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboundRecordService inboundRecordService;

    @Test
    void latestRecord_withHeader_isOk() throws Exception {
        when(inboundRecordService.latestRecord(DATA_SOURCE_ID,
                                               ACCESS_CODE,
                                               InboundProvisioningType.REST_BEARER)).thenReturn(INBOUND_RECORD_DTO);

        mockMvc.perform(get("/inbound/latest/" + DATA_SOURCE_ID)
                                .header("X-API-Key", ACCESS_CODE)
               )
               .andExpect(status().isOk());
    }

    @Test
    void latestRecord_withQueryParam_isOk() throws Exception {
        when(inboundRecordService.latestRecord(DATA_SOURCE_ID,
                                               ACCESS_CODE,
                                               InboundProvisioningType.REST_API_TOKEN)).thenReturn(INBOUND_RECORD_DTO);

        mockMvc.perform(get("/inbound/latest/" + DATA_SOURCE_ID + "?apiKey=" + ACCESS_CODE))
               .andExpect(status().isOk());
    }

    @Test
    void latestRecord_withMissingToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/inbound/latest/" + DATA_SOURCE_ID))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void latestRecord_withEmptyToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/inbound/latest/" + DATA_SOURCE_ID)
                                .header("X-API-Key", "")
               )
               .andExpect(status().isUnauthorized());
    }

    @Test
    void latestRecord_withUnsupportedTransformation_isNotImplemented() throws Exception {
        when(inboundRecordService.latestRecord(DATA_SOURCE_ID, ACCESS_CODE, InboundProvisioningType.REST_BEARER))
                .thenThrow(new UnsupportedInboundRecordTransformationException(
                        AiidaSchema.MIN_MAX_ENVELOPE_CIM_V1_12,
                        InboundMessageFormat.OPENADR_3_1
                ));

        mockMvc.perform(get("/inbound/latest/" + DATA_SOURCE_ID)
                                .header("X-API-Key", ACCESS_CODE))
               .andExpect(status().isNotImplemented());
    }
}
