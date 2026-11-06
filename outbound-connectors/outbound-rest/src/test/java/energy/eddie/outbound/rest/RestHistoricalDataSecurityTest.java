// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(RestHistoricalDataSecurityTest.Config.class)
@WebAppConfiguration
class RestHistoricalDataSecurityTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private FilterChainProxy filters;

    @Test
    void allHistoricalVersionsAcceptTheSameExistingScope() throws Exception {
        var client = MockMvcBuilders.webAppContextSetup(context).addFilters(filters).build();
        for (var version : new String[]{"0_82", "1_04", "1_12"}) {
            var endpoint = "/cim_" + version + "/validated-historical-data-md";
            client.perform(get(endpoint).header("Authorization", "Bearer historical"))
                    .andExpect(status().isOk());
            client.perform(get(endpoint).header("Authorization", "Bearer unrelated"))
                    .andExpect(status().isForbidden());
            client.perform(get(endpoint)).andExpect(status().isUnauthorized());
        }
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class Config {
        @Bean
        SecurityFilterChain security(HttpSecurity http) {
            JwtDecoder decoder = token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "eligible-party")
                    .claim("scope", token.equals("historical")
                            ? "cim-validated-historical-data-md:read"
                            : "unrelated:read")
                    .build();
            return new RestSecurityConfig().restSecurityFilterChain(PathPatternRequestMatcher.withDefaults(), http,
                    request -> new CorsConfiguration(), decoder, true, true);
        }

        @Bean
        HistoricalController controller() {
            return new HistoricalController();
        }
    }

    @RestController
    static class HistoricalController {
        @GetMapping("/cim_{version}/validated-historical-data-md")
        String historical() {
            return "data";
        }
    }
}
