package energy.eddie.outbound.rest.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(AgnosticController.class)
@AutoConfigureMockMvc(addFilters = false) // disables spring security filters
class AgnosticControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void hello_returnsHelloWorld() throws Exception {
        // Given
        // When
        mockMvc.perform(get("/hello"))
               // Then
               .andExpect(content().string("Hello World!"));
    }
}