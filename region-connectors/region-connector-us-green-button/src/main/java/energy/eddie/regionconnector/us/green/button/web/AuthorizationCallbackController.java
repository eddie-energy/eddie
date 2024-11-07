package energy.eddie.regionconnector.us.green.button.web;

import energy.eddie.regionconnector.us.green.button.exceptions.UnauthorizedException;
import energy.eddie.regionconnector.us.green.button.oauth.OAuthCallback;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.us.green.button.services.PermissionRequestCreationService;
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
    private final PermissionRequestCreationService creationService;

    public AuthorizationCallbackController(
            PermissionRequestAuthorizationService authorizationService,
            PermissionRequestCreationService creationService
    ) {
        this.authorizationService = authorizationService;
        this.creationService = creationService;
    }

    @GetMapping("/authorization-callback")
    public String authorizationCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "state") String state,
            Model model
    ) {
        var oauthCallback = new OAuthCallback(Optional.ofNullable(code), Optional.ofNullable(error), state);
        LOGGER.atInfo()
              .addArgument(oauthCallback::state)
              .log("Authorization callback received for permission request {}");
        try {
            authorizationService.authorizePermissionRequest(oauthCallback);
            var dataNeedId = creationService.findDataNeedIdByPermissionId(oauthCallback.state());
            model.addAttribute(STATUS, "OK");
            dataNeedId.ifPresent(d -> model.addAttribute("dataNeedId", d));
        } catch (UnauthorizedException e) {
            model.addAttribute(STATUS, "DENIED");
        } catch (Exception e) {
            model.addAttribute(STATUS, "ERROR");
        }
        return "authorization-callback";
    }
}
