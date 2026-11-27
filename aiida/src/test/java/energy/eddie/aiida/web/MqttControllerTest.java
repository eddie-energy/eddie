// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.errors.datasource.mqtt.MqttTlsCertificateNotFoundException;
import energy.eddie.aiida.services.MqttService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MqttController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class MqttControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MqttService mqttService;

    @Test
    void tlsCertificate_returnsTlsCertificate() throws Exception {
        var tlsCertificate = mock(ByteArrayResource.class);
        when(mqttService.tlsCertificate()).thenReturn(tlsCertificate);
        mockMvc.perform(get("/mqtt/download/tls-certificate")).andExpect(status().isOk());
    }

    @Test
    void tlsCertificate_notFound() throws Exception {
        when(mqttService.tlsCertificate()).thenThrow(new MqttTlsCertificateNotFoundException("TLS certificate not found"));
        mockMvc.perform(get("/mqtt/download/tls-certificate")).andExpect(status().isNotFound());
    }
}
