package energy.eddie.exampleappbackend.controller;

import energy.eddie.exampleappbackend.service.RealTimeDataService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RequestMapping("/websocket-secret")
@Tag(name = "WebsocketSecretApi", description = "Operations related to Websocket Authentication")
@RestController
public class WebSocketSecretController {
    private final RealTimeDataService realTimeDataService;

    @GetMapping("real-time-data")
    public ResponseEntity<String> getSecretForRealTimeDataTopic(@RequestParam Long permissionId) {
        return ResponseEntity.ok(realTimeDataService.createNewSecretForRTDWebSocketTopic(permissionId));
    }
}
