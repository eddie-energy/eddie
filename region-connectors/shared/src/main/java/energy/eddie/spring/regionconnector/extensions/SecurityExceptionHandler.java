// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.EddieApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static energy.eddie.api.agnostic.GlobalConfig.ERRORS_PROPERTY_NAME;

public class SecurityExceptionHandler implements Customizer<ExceptionHandlingConfigurer<HttpSecurity>> {
    private final ObjectMapper mapper;

    public SecurityExceptionHandler(ObjectMapper mapper) {this.mapper = mapper;}

    @Override
    public void customize(ExceptionHandlingConfigurer<HttpSecurity> customizer) {
        customizer
                .authenticationEntryPoint((request, response, authException) -> {
                    // we cannot properly differentiate between unauthenticated/unauthorized because we are not directly using Spring's security context, so no specific error message can be given
                    var errors = Map.of(ERRORS_PROPERTY_NAME,
                                        List.of(new EddieApiError(
                                                "Not authenticated/authorized to access the requested resource")));

                    // when using response.sendError(), the message will be overwritten
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(mapper.writeValueAsString(errors));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    var errors = Map.of(ERRORS_PROPERTY_NAME,
                                        List.of(new EddieApiError(
                                                "Not authorized to access the requested resource")));
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(mapper.writeValueAsString(errors));
                });
    }
}
