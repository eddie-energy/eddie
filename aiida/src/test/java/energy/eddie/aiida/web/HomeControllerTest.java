package energy.eddie.aiida.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
class HomeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getHomeWithoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("/oauth2/authorization/keycloak/**"));
    }

    @Test
    void getLogin_isOk() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk());
    }

    @Test
    void getAccountWithoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/account"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("/oauth2/authorization/keycloak/**"));
    }
}