// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.web;

import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvents;
import energy.eddie.regionconnector.us.green.button.services.utility.events.UtilityEventService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {
    private final UtilityEventService utilityEventService;

    public WebhookController(UtilityEventService utilityEventService) {
        this.utilityEventService = utilityEventService;
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receiveWebhook(@RequestBody WebhookEvents events) {
        utilityEventService.receiveEvents(events.events());
        return ResponseEntity.ok().build();
    }
}
