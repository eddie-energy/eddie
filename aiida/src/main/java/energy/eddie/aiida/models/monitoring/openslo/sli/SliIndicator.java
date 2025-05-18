package energy.eddie.aiida.models.monitoring.openslo.sli;

@SuppressWarnings("NullAway")
public class SliIndicator {
    private SliRatioMetric ratioMetric;

    public SliIndicator() {

    }

    public SliIndicator(SliRatioMetric ratioMetric) {
        this.ratioMetric = ratioMetric;
    }

    public SliRatioMetric getRatioMetric() {
        return ratioMetric;
    }

    public void setRatioMetric(SliRatioMetric ratioMetric) {
        this.ratioMetric = ratioMetric;
    }
}
