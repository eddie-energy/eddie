// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.InboundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.mock;
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
    private static final String ACCESS_CODE = "test-access-code";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InboundService inboundService;

    @Test
    void latestRecord_withHeader_isOk() throws Exception {
        when(inboundService.latestRecord(DATA_SOURCE_ID, ACCESS_CODE)).thenReturn(mock(InboundRecord.class));

        mockMvc.perform(get("/inbound/latest/" + DATA_SOURCE_ID)
                                .header("X-API-Key", ACCESS_CODE)
               )
               .andExpect(status().isOk());
    }

    @Test
    void latestRecord_withQueryParam_isOk() throws Exception {
        when(inboundService.latestRecord(DATA_SOURCE_ID, ACCESS_CODE)).thenReturn(mock(InboundRecord.class));

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
}
