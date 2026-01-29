// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.config;


import energy.eddie.aiida.web.HomeController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("unused")
class OAuth2SecurityConfigurationTest {
    @Nested
    @WebMvcTest(controllers = HomeController.class)
    @AutoConfigureMockMvc
    @Import(OAuth2SecurityConfiguration.class)
    class NoCorsPropertyTest {
        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ClientRegistrationRepository unusedClientRegistrationRepository;

        @Test
        @WithMockUser
        void givenNoCorsMappingProperty_addsNoCorsMapping() throws Exception {
            mockMvc.perform(get("/").header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Nested
    @WebMvcTest(properties = {"aiida.cors.allowed-origins=https://example.com",}, controllers = HomeController.class)
    @Import(OAuth2SecurityConfiguration.class)
    class GivenCorsPropertyTest {
        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ClientRegistrationRepository unusedClientRegistrationRepository;

        @Test
        @WithMockUser
        void givenCorsMappingProperty_addsCorsHeader() throws Exception {
            mockMvc.perform(get("/").header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().string("Access-Control-Allow-Origin", "https://example.com"));
        }

        @Test
        @WithMockUser
        void givenCorsMappingProperty_withWrongOriginHeader_returnsForbidden() throws Exception {
            mockMvc.perform(get("/").header("Origin", "https://some-other-not-permitted-domain.com"))
                   .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser
        void givenCorsMappingProperty_locationHeaderIsExposed() throws Exception {
            mockMvc.perform(get("/").header("Origin", "https://example.com"))
                   .andExpect(status().isOk())
                   .andExpect(header().string("Access-Control-Expose-Headers", "Location"));
        }
    }

    @Nested
    @WebMvcTest(controllers = HomeController.class)
    @AutoConfigureMockMvc
    @Import(OAuth2SecurityConfiguration.class)
    class LogoutTest {
        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ClientRegistrationRepository clientRegistrationRepository;

        @Test
        @WithMockUser
        void givenClientRegistrationRepository_containsEndSessionUri() {
            var clientRegistration = clientRegistrationRepository.findByRegistrationId("keycloak");
            assertNotNull(clientRegistration);

            var configurationMetadata = clientRegistration.getProviderDetails().getConfigurationMetadata();

            assertThat(configurationMetadata)
                    .containsExactlyEntriesOf(Map.of("end_session_endpoint", "https://auth.aiida.energy/logout"));
        }
    }
}
