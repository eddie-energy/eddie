// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.web;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.exceptions.EsValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(PermissionControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerAdviceTest {
    @MockitoBean
    private PermissionController controller;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testNlValidationException_returnsBadRequest() throws Exception {
        // Given
        when(controller.requestPermission(any()))
                .thenThrow(new EsValidationException(new AttributeError("nif", "msg")));
        var content = new PermissionRequestForCreation("cid", "dnid", "1000000T", "mid");

        // When
        mockMvc.perform(post("/permission-request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(content)))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors.[0].message").value("msg"));
    }
}