// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.regionconnector.de.eta.oauth.OAuthCallback;
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
        LOGGER.info("Received OAuth callback for permission request {}", state);

        try {
            // ETA+ uses 'token' instead of the standard OAuth 'code' parameter
            var callback = new OAuthCallback(
                    Optional.ofNullable(token),
                    Optional.ofNullable(error),
                    state);

            authorizationService.authorizePermissionRequest(callback);

            model.addAttribute("success", true);
            model.addAttribute("message", "Authorization successful");
            return "oauth-callback";
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Permission request not found: {}", state, e);
            model.addAttribute("success", false);
            model.addAttribute("message", "Permission request not found");
            return "oauth-callback";
        } catch (Exception e) {
            LOGGER.error("Error processing OAuth callback", e);
            model.addAttribute("success", false);
            model.addAttribute("message", "Authorization failed");
            return "oauth-callback";
        }
    }
}
