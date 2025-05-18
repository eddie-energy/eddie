package energy.eddie.aiida.models.monitoring.openslo.sli;

@SuppressWarnings("NullAway")
public class SliSpec {
    private String description;
    private SliIndicator indicator;

    public SliSpec() {

    }

    public SliSpec(String description, SliIndicator indicator) {
        this.description = description;
        this.indicator = indicator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SliIndicator getIndicator() {
        return indicator;
    }

    public void setIndicator(SliIndicator indicator) {
        this.indicator = indicator;
    }
}
