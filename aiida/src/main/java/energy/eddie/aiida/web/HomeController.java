// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

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
}
