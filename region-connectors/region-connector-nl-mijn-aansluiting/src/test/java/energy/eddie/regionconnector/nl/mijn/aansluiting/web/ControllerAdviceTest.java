package energy.eddie.regionconnector.nl.mijn.aansluiting.web;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PermissionRequestService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URISyntaxException;
import java.security.PrivateKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(ControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class ControllerAdviceTest {
    @MockitoBean
    private PkceClientController controller;
    @MockitoBean
    @SuppressWarnings("unused")
    private PrivateKey ignored;
    @MockitoBean
    @SuppressWarnings("unused")
    private PermissionEventRepository ignoredEventRepo;
    @MockitoBean
    @SuppressWarnings("unused")
    private NlPermissionRequestRepository ignoredPermissionRequestRepo;
    @MockitoBean
    @SuppressWarnings("unused")
    private PermissionRequestService ignoredPermissionRequestService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testURISyntaxExceptionAdvice() throws Exception {
        // Given
        when(controller.callback(any(), any()))
                .thenThrow(URISyntaxException.class);

        // When
        mockMvc.perform(get("/oauth2/code/mijn-aansluiting")
                                .cookie(new Cookie("EDDIE-SESSION-ID", "asdf")))
               // Then
               .andExpect(status().isBadRequest());
    }
}