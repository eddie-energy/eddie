package energy.eddie.aiida.datasources.simulation.configs;

import energy.eddie.aiida.datasources.api.configs.AiidaDataSourceConfig;

public record SimDataSourceConfig(boolean enabled, String id,
                                  int simulationPeriodInSeconds) implements AiidaDataSourceConfig {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimDataSourceConfig that)) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
