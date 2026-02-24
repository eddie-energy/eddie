// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.config.OAuth2SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(OAuth2SecurityConfiguration.class)
@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc
class HomeControllerTest {
    @Value("${aiida.public.url}")
    private String aiidaPublicUrl;
    @Value("${aiida.keycloak.url.external}")
    private String keycloakUrl;
    @Value("${aiida.keycloak.realm}")
    private String keycloakRealm;
    @Value("${aiida.keycloak.client}")
    private String keycloakClient;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testIndexIncludesModelAttributes() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(view().name("index"))
               .andExpect(model().attribute("aiidaPublicUrl", aiidaPublicUrl))
               .andExpect(model().attribute("keycloakUrl", keycloakUrl))
               .andExpect(model().attribute("keycloakRealm", keycloakRealm))
               .andExpect(model().attribute("keycloakClient", keycloakClient));
    }
}