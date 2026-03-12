package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.regionconnector.de.eta.auth.AuthCallback;
import energy.eddie.regionconnector.de.eta.service.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthorizationCallbackController {
    public static final String STATUS = "status";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationCallbackController.class);

    private final PermissionRequestAuthorizationService authorizationService;

    public AuthorizationCallbackController(PermissionRequestAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/authorization-callback")
    public String handleCallback(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String error,
            @RequestParam String state,
            Model model) {
        LOGGER.info("Received Auth callback for permission request");

        try {
            // ETA+ uses 'token' instead of the standard Auth 'code' parameter
            var callback = new AuthCallback(
                    Optional.ofNullable(token),
                    Optional.ofNullable(error),
                    state);

            authorizationService.authorizePermissionRequest(callback);
            model.addAttribute(STATUS, "OK");
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Permission request not found: {}", state, e);
            model.addAttribute(STATUS, "ERROR");
        } catch (Exception e) {
            LOGGER.error("Error processing Auth callback", e);
            model.addAttribute(STATUS, "ERROR");
        }

        return "authorization-callback";
    }
}
