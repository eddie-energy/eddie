package energy.eddie.aiida.models.monitoring.openslo.sli;

@SuppressWarnings("NullAway")
public class SliRatioMetric {
    private SliRatioMetricValue good;
    private SliRatioMetricValue total;

    public SliRatioMetric() {

    }

    public SliRatioMetric(SliRatioMetricValue good, SliRatioMetricValue total) {
        this.good = good;
        this.total = total;
    }

    public SliRatioMetricValue getGood() {
        return good;
    }

    public void setGood(SliRatioMetricValue good) {
        this.good = good;
    }

    public SliRatioMetricValue getTotal() {
        return total;
    }

    public void setTotal(SliRatioMetricValue total) {
        this.total = total;
    }
}
