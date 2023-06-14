package energy.eddie.framework.web;

import com.google.inject.Inject;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.framework.MetadataService;
import energy.eddie.framework.PermissionService;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PermissionFacade implements JavalinPathHandler {

    private final Logger logger = LoggerFactory.getLogger(PermissionFacade.class);
    private final List<WsContext> users = new ArrayList<>();
    private final MetadataService metadataService;
    @Inject
    public PermissionFacade(MetadataService metadataService, PermissionService permissionService) {
        this.metadataService = metadataService;
        Optional.ofNullable(permissionService.getConnectionStatusMessageStream())
                .ifPresent(this::subscribe);
    }

    @Override
    public void registerPathHandlers(Javalin app) {
        app.get("/api/region-connectors-metadata", ctx -> ctx.json(metadataService.getRegionConnectorMetadata()));
        app.ws("/api/connection-status-messages", ws -> {
                    ws.onConnect(users::add);
                    ws.onClose(users::remove);
                }
        );
    }

    private void subscribe(Flux<ConnectionStatusMessage> stream) {
        logger.info("Subscribing to connection status message stream");
        stream.subscribe(message -> {
            logger.debug("Incoming messages: {}", message);
            users.forEach(ctx -> ctx.send(message));
        });
    }
}
