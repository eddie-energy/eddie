package energy.eddie.aiida.models.record;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class AiidaRecordFactory {
    private static final Map<String, Class<? extends AiidaRecord>> RECORD_MAP = new HashMap<>();

    static {
        RECORD_MAP.put("1.7.0", IntegerAiidaRecord.class);
        RECORD_MAP.put("2.7.0", IntegerAiidaRecord.class);
        RECORD_MAP.put("1.8.0", IntegerAiidaRecord.class);
        RECORD_MAP.put("2.8.0", IntegerAiidaRecord.class);
        RECORD_MAP.put("C.1.0", StringAiidaRecord.class);       // Meter serial number
        RECORD_MAP.put("1-0:1.8.0", IntegerAiidaRecord.class);  // Active energy consumed
        RECORD_MAP.put("1-0:2.8.0", IntegerAiidaRecord.class);  // Active energy returned to the grid
        RECORD_MAP.put("1-0:1.7.0", IntegerAiidaRecord.class);  // Positive active instantaneous power
        RECORD_MAP.put("1-0:2.7.0", IntegerAiidaRecord.class);  // Negative active instantaneous power
        RECORD_MAP.put("1-0:3.7.0", IntegerAiidaRecord.class);  // Positive reactive instantaneous power
        RECORD_MAP.put("1-0:3.8.1", IntegerAiidaRecord.class);  // Positive reactive energy in tariff I
        RECORD_MAP.put("1-0:4.7.0", IntegerAiidaRecord.class);  // Negative reactive instantaneous power
        RECORD_MAP.put("1-0:4.8.1", IntegerAiidaRecord.class);  // Negative reactive energy in tariff I
        RECORD_MAP.put("0-0:96.1.0", StringAiidaRecord.class);
        RECORD_MAP.put("0-0:1.0.0", IntegerAiidaRecord.class);
        RECORD_MAP.put("0-0:2.0.0", IntegerAiidaRecord.class);
        RECORD_MAP.put("0-0:2.0.1", IntegerAiidaRecord.class);  // No description on the internet about this code
    }

    private AiidaRecordFactory() {
    }

    /**
     * Creates an instance of an inheritor of {@link AiidaRecord}, whereas the {@code obisCode} parameter determines which
     * concrete class will be used.
     * Note that the {@code value} parameter needs to be of appropriate type for the concrete class,
     * e.g. for an {@code IntegerAiidaRecord}, the value has to be an Integer.
     * The {@link AiidaRecordFactory} contains a map of OBIS codes and the concrete class that is used for it.
     *
     * @param obisCode  OBIS code of the record. Decides the returned datatype.
     * @param timestamp Timestamp, when the measurement was taken.
     * @param value     Value of the measurement.
     * @return Instance of an inheritor of AiidaRecord with the parameters set.
     * @throws IllegalArgumentException Thrown if the type of {@code value} is not appropriate for the
     *                                  concrete AiidaRecord class or if a not mapped OBIS code is passed in.
     */
    public static AiidaRecord createRecord(String obisCode, Instant timestamp, Object value) throws IllegalArgumentException {
        Class<? extends AiidaRecord> recordClass = RECORD_MAP.get(obisCode);

        if (recordClass == null) {
            throw new IllegalArgumentException("No definition which AiidaRecord subclass should be used for OBIS code %s".formatted(obisCode));
        }

        if (recordClass == IntegerAiidaRecord.class) {
            if (value instanceof Integer i)
                return new IntegerAiidaRecord(timestamp, obisCode, i);

            throw new IllegalArgumentException("value must be of type Integer for IntegerAiidaRecord");
        }

        if (recordClass == StringAiidaRecord.class) {
            if (value instanceof String s)
                return new StringAiidaRecord(timestamp, obisCode, s);

            throw new IllegalArgumentException("value must be of type String for StringAiidaRecord");
        }

        throw new IllegalArgumentException("No implementation that creates a %s in method createRecord(String obisCode, Instant timestamp, Object value)".formatted(recordClass.getName()));
    }
}
