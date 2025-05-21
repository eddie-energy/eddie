package energy.eddie.aiida.models.monitoring.evaluation.compliance;

import java.util.List;

public enum ServiceStatus {
    COMPLIANT,
    PARTIALLY_COMPLIANT,
    VIOLATION,
    PENDING,
    NO_METRICS,
    PARTIAL_METRICS;

    static public ServiceStatus computeServiceStatus(List<EvaluationStatus> statuses) {
        long total = statuses.size();
        long compliant = statuses.stream().filter(s -> s == EvaluationStatus.COMPLIANT).count();
        long violation = statuses.stream().filter(s -> s == EvaluationStatus.VIOLATION).count();
        long pending = statuses.stream().filter(s -> s == EvaluationStatus.PENDING).count();
        long noMetrics = statuses.stream().filter(s -> s == EvaluationStatus.NO_METRICS).count();

        if (violation > 0 && compliant > 0) return ServiceStatus.PARTIALLY_COMPLIANT;
        if (violation == total) return ServiceStatus.VIOLATION;
        if (pending > 0 && violation == 0) return ServiceStatus.PENDING;
        if (noMetrics == total) return ServiceStatus.NO_METRICS;
        if (noMetrics > 0 && compliant > 0) return ServiceStatus.PARTIAL_METRICS;
        return ServiceStatus.COMPLIANT;
    }
}

