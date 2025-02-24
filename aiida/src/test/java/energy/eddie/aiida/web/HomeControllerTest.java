package energy.eddie.aiida.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
class HomeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final String connectionId = "CONN_" + UUID.randomUUID();
    private final OidcUser oidcUser = new DefaultOidcUser(
            AuthorityUtils.createAuthorityList("ROLE_USER"),
            new OidcIdToken("token",
                            Instant.now(),
                            Instant.now().plusSeconds(60),
                            Map.of("sub", "aiida", "preferred_username", "aiida")),
            new OidcUserInfo(Map.of("given_name", "John", "family_name", "Doe"))
    );

    @Test
    void getHome_withOidcUser() throws Exception {
        mockMvc.perform(get("/").with(oauth2Login().oauth2User(oidcUser)))
               .andExpect(status().isOk())
               .andExpect(model().attribute("isAuthenticated", true))
               .andExpect(model().attribute("oidcUser", oidcUser))
               .andExpect(model().attribute("userInitials", "JD"));
    }

    @Test
    @WithMockUser
    void getHome_createsCookieIfNotPresent() throws Exception {
        mockMvc.perform(get("/").with(csrf()))
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
        mockMvc.perform(get("/").with(csrf()))
               .andExpect(status().isOk())
               .andExpect(model().attribute("isAuthenticated", true));
    }

    @Test
    void getHomeWithoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
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
               .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
    }

    @Test
    @WithMockUser
    void getAccountAuthentication_isFoundAndHasRedirectUri() throws Exception {
        mockMvc.perform(get("/account").with(csrf()))
               .andExpect(status().isFound())
               .andExpect(redirectedUrl("https://auth.aiida.energy/account"));
    }

    @Test
    void getInstallerWithoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("**/oauth2/authorization/keycloak"));
    }

    @Test
    @WithMockUser
    void getInstallerWithAuthentication_isOk() throws Exception {
        mockMvc.perform(get("/installer"))
               .andExpect(status().isOk());
    }
}