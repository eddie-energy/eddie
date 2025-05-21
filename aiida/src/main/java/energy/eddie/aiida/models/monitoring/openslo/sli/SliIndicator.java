package energy.eddie.aiida.models.monitoring.openslo.sli;

@SuppressWarnings("NullAway")
public class SliIndicator {
    private SliRatioMetric ratioMetric;
    private SliThresholdMetric thresholdMetric;

    public SliIndicator() {}

    public SliIndicator(SliRatioMetric ratioMetric) {
        this.ratioMetric = ratioMetric;
    }

    public SliIndicator(SliThresholdMetric thresholdMetric) {
        this.thresholdMetric = thresholdMetric;
    }

    public SliRatioMetric getRatioMetric() {
        return ratioMetric;
    }

    public void setRatioMetric(SliRatioMetric ratioMetric) {
        this.ratioMetric = ratioMetric;
    }

    public SliThresholdMetric getThresholdMetric() {
        return thresholdMetric;
    }

    public void setThresholdMetric(SliThresholdMetric thresholdMetric) {
        this.thresholdMetric = thresholdMetric;
    }

    public boolean isThresholdMetric() {
        return thresholdMetric != null;
    }

    public boolean isRatioMetric() {
        return ratioMetric != null;
    }
}
