// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.errors.GlobalExceptionHandler;
import energy.eddie.aiida.models.user.UserSettings;
import energy.eddie.aiida.services.UserSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserSettingsController.class)
class UserSettingsControllerTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");

    @MockitoBean
    private UserSettingsService userSettingsService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void givenSettings_returnsSettings() throws Exception {
        when(userSettingsService.getUserSettings()).thenReturn(new UserSettings(USER_ID, "user@example.com"));

        mockMvc.perform(get("/user-settings"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
               .andExpect(jsonPath("$.contactEmail").value("user@example.com"));
    }

    @Test
    @WithMockUser
    void givenValidEmail_updatesSettings() throws Exception {
        when(userSettingsService.updateUserSettings("user@example.com")).thenReturn(new UserSettings(USER_ID,
                                                                                                     "user@example.com"));

        mockMvc.perform(put("/user-settings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"contactEmail\":\"user@example.com\"}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.contactEmail").value("user@example.com"));
    }

    @Test
    @WithMockUser
    void givenInvalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/user-settings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"contactEmail\":\"not-an-email\"}"))
               .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class UserSettingsControllerTestConfiguration {
        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }
}
