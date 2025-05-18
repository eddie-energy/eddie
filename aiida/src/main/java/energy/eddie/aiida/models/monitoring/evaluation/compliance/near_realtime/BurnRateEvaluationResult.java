package energy.eddie.aiida.models.monitoring.evaluation.compliance.near_realtime;

public record BurnRateEvaluationResult(String fromTimestamp, String toTimestamp, String lookbackWindow, double currentBurnRate, double threshold) {
}
