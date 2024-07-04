package energy.eddie.examples.newexampleapp.web;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, HttpServletRequest request, HttpServletResponse response) {
        // Check if connection IDs already exist in the cookies
        Cookie[] cookies = request.getCookies();
        String connectionIDH = null;
        String connectionIDA = null;
        String cookieHName = "connectionIDHistorical";
        String cookieAName = "connectionIDAiida";

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieHName)) {
                    connectionIDH = cookie.getValue();
                } else if (cookie.getName().equals(cookieAName)) {
                    connectionIDA = cookie.getValue();
                }
            }
        }
        // If not, generate new ones and store them in the cookies
        connectionIDH = createCookie(response, connectionIDH, cookieHName);
        connectionIDA = createCookie(response, connectionIDA, cookieAName);

        // Add the connection IDs to the model
        model.addAttribute(cookieHName, connectionIDH);
        model.addAttribute(cookieAName, connectionIDA);


        model.addAttribute("dataNeedID", "b35dc783-a25e-487a-a914-6064bdd7feb5"); //Should generate this properly next time
        model.addAttribute("aiidaID", "5dc71d7e-e8cd-4403-a3a8-d3c095c97a84"); //Should generate this properly next time
        return "index";
    }

    protected String createCookie(HttpServletResponse response, @Nullable String connectionID, String cookieH) {
        if (connectionID == null) {
            connectionID = "CONN_" + UUID.randomUUID();
            Cookie cookie = new Cookie(cookieH, connectionID);
            cookie.setMaxAge(60 * 60 * 24); // Set cookie to expire after 24 hours
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            response.addCookie(cookie);
        }
        return connectionID;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}