package energy.eddie.aiida.models.monitoring.openslo.slo;


@SuppressWarnings("NullAway")
public class SloMetadataLabels {
    private String service;

    public SloMetadataLabels() {
    }

    public SloMetadataLabels(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }
}