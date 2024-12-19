package energy.eddie.aiida.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@Controller
public class HomeController {
    public static final Duration MAX_CONNECTION_ID_LIFETIME = Duration.ofHours(24);
    public static final String CONNECTION_ID_COOKIE_NAME = "connectionId";

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());

        if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
            model.addAttribute("oidcUser", oidcUser);

            var initials = String.valueOf(oidcUser.getGivenName().charAt(0)) + oidcUser.getFamilyName().charAt(0);
            model.addAttribute("userInitials", initials);
        }

        String connectionId = getConnectionIdFromCookiesIfPresent(request);
        connectionId = createCookie(response, connectionId);

        model.addAttribute(CONNECTION_ID_COOKIE_NAME, connectionId);
        return "index";
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

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/account")
    public void account(HttpServletResponse resp, @Value("${aiida.keycloak.account-uri:}") String accountUri) throws IOException {
        resp.sendRedirect(accountUri);
    }
}
