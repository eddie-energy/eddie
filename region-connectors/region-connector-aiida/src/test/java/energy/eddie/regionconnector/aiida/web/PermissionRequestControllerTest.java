package energy.eddie.regionconnector.aiida.web;

import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PermissionRequestController.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class PermissionRequestControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AiidaRegionConnectorService service;

    @Nested
    @DisplayName("Test new permission request")
    class NewPermissionTest {
        @Test
        void givenNoRequestBody_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/permission-request"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("Failed to read request")
                    )));
        }

        @Test
        void givenMissingConnectionId_returnsBadRequest() throws Exception {
            var json = "{\"dataNeedId\":\"1\"}";

            mockMvc.perform(post("/permission-request")
                            .content(json)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("ConnectionId must not be empty")
                    )));
        }

        @Test
        void givenStateTransitionException_returnsInternalServerError() throws Exception {
            var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\"}";

            when(service.createNewPermission(any())).thenThrow(new PastStateException(mock(PermissionRequestState.class)));

            mockMvc.perform(post("/permission-request")
                            .content(json)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errors", allOf(
                            iterableWithSize(1),
                            hasItem("An error occurred while trying to transition a permission request to a new state")
                    )));
        }

        @Test
        void givenAdditionalNotNeededInformation_isIgnored() throws Exception {
            var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"11\",\"extra\":\"information\"}";

            mockMvc.perform(post("/permission-request")
                            .content(json)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(service).createNewPermission(any());
        }

        @Test
        void givenValidInput_asExpected() throws Exception {
            var json = "{\"connectionId\":\"Hello My Test\",\"dataNeedId\":\"1\"}";

            mockMvc.perform(post("/permission-request")
                            .content(json)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(service).createNewPermission(any());
        }
    }
}