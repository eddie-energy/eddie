package energy.eddie.aiida.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final String connectionId = "CONN_" + UUID.randomUUID();

    @Test
    @WithMockUser
    void getHome_createsCookieIfNotPresent() throws Exception {
        mockMvc.perform(get("/")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(cookie().exists(HomeController.CONNECTION_ID_COOKIE_NAME))
                .andExpect(model().attributeExists(HomeController.CONNECTION_ID_COOKIE_NAME));
    }

    @Test
    @WithMockUser
    void getHome_storesCookieInModelIfPresent() throws Exception {
        mockMvc.perform(get("/")
                        .with(csrf())
                        .cookie(new Cookie(HomeController.CONNECTION_ID_COOKIE_NAME, connectionId))
                )
                .andExpect(status().isOk())
                // the request does not have the cookie, because it is already present in the session
                .andExpect(model().attribute(HomeController.CONNECTION_ID_COOKIE_NAME, connectionId));
    }

    @Test
    @WithMockUser
    void getHomeWithAuthentication_authenticatedModelTrue() throws Exception {
        mockMvc.perform(get("/")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(model().attribute("isAuthenticated", true));
    }

    @Test
    void getHomeWithoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getLogin_isOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

}