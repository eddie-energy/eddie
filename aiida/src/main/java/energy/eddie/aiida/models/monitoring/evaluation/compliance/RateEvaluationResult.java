package energy.eddie.aiida.models.monitoring.evaluation.compliance;

public record RateEvaluationResult(
        double currentRate,
        double threshold) {}
