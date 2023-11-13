package energy.eddie.regionconnector.aiida.web;

import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PermissionRequestController.class)
class PermissionRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AiidaRegionConnectorService service;

    @Test
    void getConnectorElementJavascript() throws Exception {
        String result = mockMvc.perform(get("/region-connectors/aiida/ce.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result).contains("<h1>THIS IS AIIDA!!!</h1>");
    }

    @Nested
    @DisplayName("Test new permission request")
    class NewPermissionTest {
        @Test
        void givenNoRequestBody_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/region-connectors/aiida/permission-request"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("Failed to read request")
                    )));
        }

        @Test
        void givenMissingConnectionId_returnsBadRequest() throws Exception {
            var json = "{\"dataNeedId\":\"1\",\"startTime\":1695095000,\"expirationTime\":1695200000}";

            mockMvc.perform(post("/region-connectors/aiida/permission-request")
                            .content(json)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("ConnectionId must not be empty")
                    )));
        }

        @Test
        void givenStartTimeAfterEndTime_returnsBadRequest() throws Exception {
            var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\",\"startTime\":1695095000,\"expirationTime\":1000000000}";

            mockMvc.perform(post("/region-connectors/aiida/permission-request")
                            .content(json)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("startTime must be before expirationTime")
                    )));
        }

        @Test
        void givenValidInput_asExpected() throws Exception {
            var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\",\"startTime\":1695095000,\"expirationTime\":1705095000}";

            mockMvc.perform(post("/region-connectors/aiida/permission-request")
                            .content(json)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(service).createNewPermission(any());
        }
    }
}