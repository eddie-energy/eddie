package energy.eddie.api.agnostic.data.needs;

import java.util.Map;
import java.util.Set;

public sealed interface MultipleDataNeedCalculationResult {
    record CalculationResult(Map<String, DataNeedCalculationResult> result)
            implements MultipleDataNeedCalculationResult {}

    record InvalidDataNeedCombination(Set<String> offendingDataNeedIds, String message)
            implements MultipleDataNeedCalculationResult {}
}
