package energy.eddie.core.dtos;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculation;

import java.util.Map;
import java.util.Set;

public sealed interface MultipleDataNeedsOrErrorResult {
    record MultipleDataNeeds(Map<String, DataNeedCalculation> result) implements MultipleDataNeedsOrErrorResult {}

    record MultipleDataNeedsError(Set<String> offendingDataNeedIds,
                                  String message) implements MultipleDataNeedsOrErrorResult {}
}
