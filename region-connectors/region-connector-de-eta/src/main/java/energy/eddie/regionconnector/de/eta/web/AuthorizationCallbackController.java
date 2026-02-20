// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.regionconnector.de.eta.oauth.OAuthCallback;
import energy.eddie.regionconnector.de.eta.service.PermissionRequestAuthorizationService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Controller
public class AuthorizationCallbackController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationCallbackController.class);

    private final PermissionRequestAuthorizationService authorizationService;

    public AuthorizationCallbackController(PermissionRequestAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping(value = "/authorization-callback", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String handleCallback(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String error,
            @RequestParam String state) {
        LOGGER.info("Received OAuth callback for permission request {}", state);

        boolean success;
        String message;

        try {
            // ETA+ uses 'token' instead of the standard OAuth 'code' parameter
            var callback = new OAuthCallback(
                    Optional.ofNullable(token),
                    Optional.ofNullable(error),
                    state);

            authorizationService.authorizePermissionRequest(callback);
            success = true;
            message = "Authorization successful";
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Permission request not found: {}", state, e);
            success = false;
            message = "Permission request not found";
        } catch (Exception e) {
            LOGGER.error("Error processing OAuth callback", e);
            success = false;
            message = "Authorization failed";
        }

        return renderCallbackPage(success, message);
    }

    private String renderCallbackPage(boolean success, String message) {
        String color = success ? "#10b981" : "#ef4444";
        String icon = success ? "✓" : "✗";
        String title = success ? "Authorization Successful" : "Authorization Failed";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>OAuth Authorization</title>
                    <style>
                        body { font-family: sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background: #f3f4f6; }
                        .container { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); max-width: 400px; text-align: center; }
                        .icon { font-size: 4rem; margin-bottom: 1rem; color: %s; }
                        h1 { margin: 0 0 1rem 0; font-size: 1.5rem; color: %s; }
                        p { color: #6b7280; line-height: 1.6; }
                        .close-button { margin-top: 1.5rem; padding: 0.75rem 1.5rem; background: #017aa0; color: white; border: none; border-radius: 4px; cursor: pointer; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="icon">%s</div>
                        <h1>%s</h1>
                        <p>%s</p>
                        <button class="close-button" onclick="window.close()">Close Window</button>
                    </div>
                    <script>
                        // Auto-close after 3 seconds if successful
                        if (%b) {
                            setTimeout(() => window.close(), 3000);
                        }
                    </script>
                </body>
                </html>
                """
                .formatted(color, color, icon, title, message, success);
    }
}
