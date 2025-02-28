package energy.eddie.aiida.models.record;

import energy.eddie.aiida.utils.ObisCode;

import java.util.List;

public class AiidaRecordValidator {
    private AiidaRecordValidator() {
        // Util Class
    }

    public static List<String> checkInvalidDataTags(AiidaRecord rec) {
        return rec.aiidaRecordValues()
                  .stream()
                  .filter(aiidaRecordValue -> aiidaRecordValue.dataTag() == ObisCode.UNKNOWN)
                  .map(AiidaRecordValue::rawTag)
                  .toList();
    }
}
