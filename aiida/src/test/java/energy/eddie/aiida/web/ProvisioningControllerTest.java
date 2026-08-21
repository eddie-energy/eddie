// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.provisioning.MqttClientProvisioningTypePatchDto;
import energy.eddie.aiida.dtos.provisioning.MqttProvisioningConnectionDto;
import energy.eddie.aiida.dtos.provisioning.ProvisioningTypePatchDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.inbound.ProvisioningConfigurationException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.services.ProvisioningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ProvisioningController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
class ProvisioningControllerTest {
    private static final UUID PERMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProvisioningService provisioningService;

    @Test
    void patchInboundProvisioningType_withMqttClient_returnsConnectionDto() throws Exception {
        var response = new MqttProvisioningConnectionDto(
                "mqtt://broker.example.test",
                "mqtt-user",
                "mqtt-password",
                "aiida/inbound/test"
        );
        when(provisioningService.changeProvisioningType(
                eq(PERMISSION_ID),
                argThat(dto -> dto.type() == InboundProvisioningType.MQTT_CLIENT)
        )).thenReturn(response);

        mockMvc.perform(validPatch())
               .andExpect(status().isOk())
               .andExpect(jsonPath("host").value("mqtt://broker.example.test"))
               .andExpect(jsonPath("username").value("mqtt-user"))
               .andExpect(jsonPath("password").value("mqtt-password"))
               .andExpect(jsonPath("topic").value("aiida/inbound/test"));

        verify(provisioningService).changeProvisioningType(
                eq(PERMISSION_ID),
                argThat(ProvisioningControllerTest::hasMqttClientPatchValues)
        );
    }

    @Test
    void patchInboundProvisioningType_withUnknownPermission_returnsNotFound() throws Exception {
        when(provisioningService.changeProvisioningType(eq(PERMISSION_ID), any()))
                .thenThrow(new PermissionNotFoundException(PERMISSION_ID));

        mockMvc.perform(validPatch())
               .andExpect(status().isNotFound());
    }

    @Test
    void patchInboundProvisioningType_withInvalidDataSourceType_returnsBadRequest() throws Exception {
        when(provisioningService.changeProvisioningType(eq(PERMISSION_ID), any()))
                .thenThrow(new InvalidDataSourceTypeException());

        mockMvc.perform(validPatch())
               .andExpect(status().isBadRequest());
    }

    @Test
    void patchInboundProvisioningType_withoutMqttConfiguration_returnsConflict() throws Exception {
        when(provisioningService.changeProvisioningType(eq(PERMISSION_ID), any()))
                .thenThrow(new ProvisioningConfigurationException(
                        PERMISSION_ID,
                        InboundProvisioningType.MQTT_CLIENT
                ));

        mockMvc.perform(validPatch())
               .andExpect(status().isConflict());
    }

    @Test
    void patchInboundProvisioningType_withBlankMqttClientConfiguration_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/provisioning/permission/" + PERMISSION_ID + "/patchInboundProvisioning")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                 {
                                                   "type": "MQTT_CLIENT",
                                                   "host": " ",
                                                   "username": "mqtt-user",
                                                   "password": "mqtt-password",
                                                   "topic": "aiida/inbound/test"
                                                 }
                                                 """))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("errors").isArray());

        verifyNoInteractions(provisioningService);
    }

    @Test
    void patchInboundProvisioningType_withMissingMqttClientConfiguration_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/provisioning/permission/" + PERMISSION_ID + "/patchInboundProvisioning")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                 {
                                                   "type": "MQTT_CLIENT"
                                                 }
                                                 """))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("errors").isArray());

        verifyNoInteractions(provisioningService);
    }

    @Test
    void patchInboundProvisioningType_withTypeOnly_returnsOk() throws Exception {
        when(provisioningService.changeProvisioningType(eq(PERMISSION_ID), any()))
                .thenReturn(MqttProvisioningConnectionDto.empty());

        mockMvc.perform(patch("/provisioning/permission/" + PERMISSION_ID + "/patchInboundProvisioning")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                 {
                                                   "type": "REST_BEARER"
                                                 }
                                                 """))
               .andExpect(status().isOk());

        verify(provisioningService).changeProvisioningType(
                eq(PERMISSION_ID),
                argThat(dto -> dto.type() == InboundProvisioningType.REST_BEARER)
        );
    }

    @Test
    void getProvisioningTypes_returnsAllProvisioningTypes() throws Exception {
        mockMvc.perform(get("/provisioning/types"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("provisioningTypes").isArray())
               .andExpect(jsonPath("provisioningTypes").value(hasItems(
                       "REST_BEARER",
                       "REST_API_TOKEN",
                       "MQTT_SERVER",
                       "MQTT_CLIENT"
               )));
    }

    @Test
    void resetServerModePassword_returnsGeneratedPassword() throws Exception {
        when(provisioningService.resetServerModePassword(PERMISSION_ID))
                .thenReturn(new MqttProvisioningConnectionDto(
                        "mqtt://broker.example.test",
                        "mqtt-user",
                        "new-password",
                        "aiida/inbound/test"
                ));

        mockMvc.perform(post("/provisioning/permission/" + PERMISSION_ID + "/regenerate-server-provisioning-password"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("host").value("mqtt://broker.example.test"))
               .andExpect(jsonPath("username").value("mqtt-user"))
               .andExpect(jsonPath("password").value("new-password"))
               .andExpect(jsonPath("topic").value("aiida/inbound/test"));

        verify(provisioningService).resetServerModePassword(PERMISSION_ID);
    }

    private static boolean hasMqttClientPatchValues(ProvisioningTypePatchDto dto) {
        return dto instanceof MqttClientProvisioningTypePatchDto(
                InboundProvisioningType type, String host, String username, String password, String topic
        )
               && type == InboundProvisioningType.MQTT_CLIENT
               && host.equals("mqtt://broker.example.test")
               && username.equals("mqtt-user")
               && password.equals("mqtt-password")
               && topic.equals("aiida/inbound/test");
    }

    private static MockHttpServletRequestBuilder validPatch() {
        return patch("/provisioning/permission/" + PERMISSION_ID + "/patchInboundProvisioning")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                 {
                                   "type": "MQTT_CLIENT",
                                   "host": "mqtt://broker.example.test",
                                   "username": "mqtt-user",
                                   "password": "mqtt-password",
                                   "topic": "aiida/inbound/test"
                                 }
                                 """);
    }
}
