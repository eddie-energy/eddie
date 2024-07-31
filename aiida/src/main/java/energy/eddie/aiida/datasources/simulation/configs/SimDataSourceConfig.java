package energy.eddie.aiida.datasources.simulation.configs;

public record SimDataSourceConfig(boolean enabled, String id, int simulationPeriodInSeconds) {
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
