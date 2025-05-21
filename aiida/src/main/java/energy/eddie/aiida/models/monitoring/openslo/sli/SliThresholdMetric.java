package energy.eddie.aiida.models.monitoring.openslo.sli;

@SuppressWarnings("NullAway")
public class SliThresholdMetric {
    private String source;
    private String query;

    public SliThresholdMetric() {}

    public SliThresholdMetric(String source, String query) {
        this.source = source;
        this.query = query;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}