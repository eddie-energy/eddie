package energy.eddie.outbound.metric.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Metrics {
    private Integer count;

    @Nullable
    private Double mean;

    @Nullable
    private Double median;

    public Metrics(Integer count) {
        this.count = count;
    }

    public Metrics(Double mean, Double median, Integer count) {
        this.mean = mean;
        this.median = median;
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    @Nullable
    public Double getMean() {
        return mean;
    }

    @Nullable
    public Double getMedian() {
        return median;
    }
}
