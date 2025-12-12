package energy.eddie.aiida.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Duration;

@Controller
public class HomeController {
    public static final Duration MAX_CONNECTION_ID_LIFETIME = Duration.ofHours(24);
    public static final String CONNECTION_ID_COOKIE_NAME = "connectionId";

    // Sonar wants to add a @PathVariable here, but we do NOT need it since we don't consume it
    @SuppressWarnings("java:S6856")
    @GetMapping(value = {"/",
            "/{path:^(?!api$)[^.]*}"})
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
        return "index";
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
