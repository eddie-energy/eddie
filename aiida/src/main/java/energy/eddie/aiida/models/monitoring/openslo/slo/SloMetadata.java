package energy.eddie.aiida.models.monitoring.openslo.slo;

@SuppressWarnings("NullAway")
public class SloMetadata {
    private String name;
    private SloMetadataLabels labels;

    public SloMetadata(String name, SloMetadataLabels labels) {
        this.name = name;
        this.labels = labels;
    }

    public SloMetadata() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SloMetadataLabels getLabels() {
        return labels;
    }

    public void setLabels(SloMetadataLabels labels) {
        this.labels = labels;
    }
}