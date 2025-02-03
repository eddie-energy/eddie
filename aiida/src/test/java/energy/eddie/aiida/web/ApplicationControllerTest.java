package energy.eddie.aiida.web;

import energy.eddie.aiida.application.information.ApplicationInformation;
import energy.eddie.aiida.services.ApplicationInformationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class ApplicationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationInformationService applicationInformationService;

    @Test
    void applicationInformation_returnsApplicationInformation() throws Exception {
        when(applicationInformationService.applicationInformation()).thenReturn(mock(ApplicationInformation.class));

        mockMvc.perform(get("/application-information")).andExpect(status().isOk());
    }
}
