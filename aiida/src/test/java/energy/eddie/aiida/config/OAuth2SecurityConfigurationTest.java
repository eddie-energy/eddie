// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.config;


import energy.eddie.aiida.web.HomeController;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuth2SecurityConfigurationTest {
    @Nested
    @WebMvcTest(controllers = HomeController.class)
    @AutoConfigureMockMvc
    @Import(OAuth2SecurityConfiguration.class)
    class NoCorsPropertyTest {
        @Autowired
        private MockMvc mockMvc;

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
}
