package energy.eddie.aiida.web;

import energy.eddie.aiida.config.KeycloakConfiguration;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@Controller
public class HomeController {
    public static final Duration MAX_CONNECTION_ID_LIFETIME = Duration.ofHours(24);
    public static final String CONNECTION_ID_COOKIE_NAME = "connectionId";

    @GetMapping("/")
    public String vue(
            Model model,
            @Value("${aiida.public.url}") String aiidaPublicUrl,
            @Value("${aiida.keycloak.url.external}") String keycloakUrl,
            @Value("${aiida.keycloak.realm}") String keycloakRealm,
            @Value("${aiida.keycloak.client}") String keycloakClient
    ) {
        model.addAttribute("aiidaPublicUrl", aiidaPublicUrl);
        model.addAttribute("keycloakUrl", keycloakUrl);
        model.addAttribute("keycloakRealm", keycloakRealm);
        model.addAttribute("keycloakClient", keycloakClient);
        return "vue";
    }

    protected @Nullable String getConnectionIdFromCookiesIfPresent(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        var connectionIdCookie = Arrays.stream(request.getCookies())
                                       .filter(cookie -> cookie.getName().equals(CONNECTION_ID_COOKIE_NAME))
                                       .findFirst();
        return connectionIdCookie.map(Cookie::getValue).orElse(null);
    }

    protected String createCookie(HttpServletResponse response, @Nullable String connectionId) {
        if (connectionId == null) {
            connectionId = "CONN_" + UUID.randomUUID();
            Cookie cookie = new Cookie(CONNECTION_ID_COOKIE_NAME, connectionId);
            cookie.setMaxAge((int) MAX_CONNECTION_ID_LIFETIME.getSeconds());
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            response.addCookie(cookie);
        }
        return connectionId;
    }

    @GetMapping("/installer")
    public String installer() {
        return "installer";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
