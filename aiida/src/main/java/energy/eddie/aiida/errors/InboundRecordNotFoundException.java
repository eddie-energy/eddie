package energy.eddie.aiida.errors;

import java.util.UUID;

public class InboundRecordNotFoundException extends Exception {
    public InboundRecordNotFoundException(UUID dataSourceId) {
        super("No inbound record found for data source with ID '%s'.".formatted(dataSourceId));
    }
}
