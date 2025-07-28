package energy.eddie.outbound.rest.tasks;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.outbound.rest.connectors.AgnosticConnector;
import energy.eddie.outbound.rest.model.ConnectionStatusMessageModel;
import energy.eddie.outbound.rest.persistence.ConnectionStatusMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConnectionStatusMessageInsertionTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStatusMessageInsertionTask.class);
    private final ConnectionStatusMessageRepository repository;

    public ConnectionStatusMessageInsertionTask(
            AgnosticConnector connector,
            ConnectionStatusMessageRepository repository
    ) {
        this.repository = repository;
        connector.getConnectionStatusMessageStream().subscribe(this::insert);
    }

    public void insert(ConnectionStatusMessage message) {
        LOGGER.debug("Inserting connection status message");
        var model = new ConnectionStatusMessageModel(message);
        repository.save(model);
    }
}
