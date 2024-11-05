package energy.eddie.regionconnector.shared.utils;

import energy.eddie.dataneeds.needs.DataNeed;

import java.util.List;
import java.util.stream.Collectors;

public class DataNeedUtils {
    public static List<String> convertDataNeedClassesToString(List<Class<? extends DataNeed>> dataNeeds) {
        return dataNeeds.stream()
                .map(Class::getName)
                .map(s -> {
                    var splitted = s.split("\\.");
                    return splitted[splitted.length - 1];
                })
                .collect(Collectors.toList());
    }
}
