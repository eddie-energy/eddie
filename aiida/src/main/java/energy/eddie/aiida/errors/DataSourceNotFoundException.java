package energy.eddie.aiida.errors;

import java.util.UUID;

public class DataSourceNotFoundException extends Exception {
    public DataSourceNotFoundException(UUID dataSourceId) {
        super("Data source not found with ID :%s".formatted(dataSourceId));
    }
}
