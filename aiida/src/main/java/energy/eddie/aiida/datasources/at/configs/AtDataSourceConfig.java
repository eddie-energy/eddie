package energy.eddie.aiida.datasources.at.configs;

public record AtDataSourceConfig(
        boolean enabled,
        String id,
        String mqttServerUri,
        String mqttSubscribeTopic
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AtDataSourceConfig that)) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
