package energy.eddie.regionconnector.cds.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.cds.dtos.CdsServerCreation;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.services.client.creation.CdsClientCreationService;
import energy.eddie.regionconnector.cds.services.client.creation.responses.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CdsController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class CdsControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CdsClientCreationService creationService;

    @Test
    void testRegisterCdsClient_returnsOk() throws Exception {
        // Given
        var url = URI.create("http://localhost:8080").toURL();
        var cdsServer = new CdsServerBuilder().build();
        when(creationService.createOAuthClients(url))
                .thenReturn(new CreatedCdsClientResponse(cdsServer));
        var body = objectMapper.writeValueAsString(new CdsServerCreation(url));

        // When
        mockMvc.perform(post("/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
               .andExpect(status().isCreated());
    }


    @ParameterizedTest
    @MethodSource("provideErrorCases")
    void testRegisterCdsClient_returnsBadRequest_onVariousErrors(
            ApiClientCreationResponse response,
            String expectedErrorMessage
    ) throws Exception {
        // Given
        var url = URI.create("http://localhost:8080").toURL();
        when(creationService.createOAuthClients(url)).thenReturn(response);
        var body = objectMapper.writeValueAsString(new CdsServerCreation(url));

        // When
        mockMvc.perform(post("/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
               // Then
               .andExpect(status().isBadRequest())
               .andExpect(content().json("{error:  \"" + expectedErrorMessage + "\"}"));
    }

    private static Stream<Arguments> provideErrorCases() {
        return Stream.of(
                Arguments.of(new NotACdsServerResponse(), "Not a CDS server"),
                Arguments.of(new AuthorizationCodeGrantTypeNotSupported(),
                             "Authorization code grant type not supported"),
                Arguments.of(new CoverageNotSupportedResponse(), "Coverage capability not supported"),
                Arguments.of(new OAuthNotSupportedResponse(), "OAuth capability not supported"),
                Arguments.of(new RefreshTokenGrantTypeNotSupported(), "Refresh token grant type not supported")
        );
    }
}