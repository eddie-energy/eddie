package energy.eddie.aiida.models.monitoring.evaluation.compliance;

public record EvaluationPeriod(String fromTimestamp,
                               String toTimestamp,
                               String lookbackWindow) { }
