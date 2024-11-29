package energy.eddie.aiida.models.record;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AiidaRecordValidator {
    private AiidaRecordValidator() {
        // Util Class
    }

    private static final Set<String> VALID_DATA_TAGS = Set.of(
            "1.7.0", "2.7.0", "1.8.0", "2.8.0", "C.1.0",
            "1-0:1.8.0", "1-0:2.8.0", "1-0:1.7.0", "1-0:2.7.0", "1-0:3.7.0",
            "1-0:3.8.1", "1-0:4.7.0", "1-0:4.8.1", "0-0:96.1.0",
            "0-0:1.0.0", "0-0:2.0.0", "0-0:2.0.1"
    );

    public static List<String> checkInvalidDataTags(AiidaRecord rec) {
        List<String> invalidDataTags = new ArrayList<>();

        for (AiidaRecordValue value : rec.aiidaRecordValue()) {
            if (!VALID_DATA_TAGS.contains(value.dataTag())) {
                invalidDataTags.add(value.rawTag());
            }
        }

        return invalidDataTags;
    }
}
