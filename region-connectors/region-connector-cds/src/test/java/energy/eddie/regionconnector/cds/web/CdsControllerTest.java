package energy.eddie.regionconnector.cds.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.cds.client.CdsApiClient;
import energy.eddie.regionconnector.cds.client.CdsApiClientFactory;
import energy.eddie.regionconnector.cds.client.responses.*;
import energy.eddie.regionconnector.cds.dtos.CdsServerCreation;
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
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class CdsControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CdsApiClientFactory factory;

    @Test
    void testRegisterCdsClient_returnsOk() throws Exception {
        // Given
        var url = URI.create("http://localhost:8080").toURL();
        when(factory.getCdsApiClient(url))
                .thenReturn(Mono.just(new CreatedApiClientResponse(new CdsApiClient())));
        var body = objectMapper.writeValueAsString(new CdsServerCreation(url));

        // When
        mockMvc.perform(post("/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
               .andExpect(status().isOk());
    }


    @ParameterizedTest
    @MethodSource("provideErrorCases")
    void testRegisterCdsClient_returnsBadRequest_onVariousErrors(
            ApiClientCreationResponse response,
            String expectedErrorMessage
    ) throws Exception {
        // Given
        var url = URI.create("http://localhost:8080").toURL();
        when(factory.getCdsApiClient(url)).thenReturn(Mono.just(response));
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