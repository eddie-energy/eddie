package energy.eddie.aiida.config;


import energy.eddie.aiida.web.HomeController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
            assertNotEquals(null, clientRegistration);

            var configurationMetadata = clientRegistration.getProviderDetails().getConfigurationMetadata();

            assertTrue(configurationMetadata.containsKey("end_session_endpoint"));
            assertEquals("https://auth.aiida.energy/logout", configurationMetadata.get("end_session_endpoint"));
        }

        @Test
        void testLogoutSuccessHandlerIsConfigured() throws Exception {
            mockMvc.perform(get("/logout").with(csrf()))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(result -> {
                       String location = result.getResponse().getHeader("Location");
                       assert location != null && location.contains("http://localhost/login");
                   });
        }
    }
}
