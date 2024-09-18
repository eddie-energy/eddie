package energy.eddie.aiida.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());

        // Check if connection IDs already exist in the cookies
        Cookie[] cookies = request.getCookies();
        String connectionId = null;
        String connectionIdCookieName = "connectionId";

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(connectionIdCookieName)) {
                    connectionId = cookie.getValue();
                }
            }
        }
        // If not, generate new ones and store them in the cookies
        connectionId = createCookie(response, connectionId, connectionIdCookieName);

        // Add the connection IDs to the model
        model.addAttribute(connectionIdCookieName, connectionId);
        return "index";
    }

    protected String createCookie(HttpServletResponse response, @Nullable String connectionId, String cookieH) {
        if (connectionId == null) {
            connectionId = "CONN_" + UUID.randomUUID();
            Cookie cookie = new Cookie(cookieH, connectionId);
            cookie.setMaxAge(60 * 60 * 24); // Set cookie to expire after 24 hours
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
}
