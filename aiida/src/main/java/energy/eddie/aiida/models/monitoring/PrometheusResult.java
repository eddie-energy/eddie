package energy.eddie.aiida.models.monitoring;

import java.util.List;
import java.util.Map;

@SuppressWarnings("NullAway")
public class PrometheusResult {
    public String status;
    public Data data;

    public static class Data {
        public String resultType;
        public List<Result> result;
    }

    public static class Result {
        public Map<String, String> metric;
        public List<Object> value; // [ <timestamp>, <value as string> ]
    }
}