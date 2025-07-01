package energy.eddie.aiida.utils;

import energy.eddie.cim.v1_04.StandardCodingSchemeTypeList;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.function.Function;

public class CimUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimUtils.class);

    private CimUtils() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    public static StandardCodingSchemeTypeList codingSchemeFromCountryCode(String countryCode) {
        return fromValue(countryCode, StandardCodingSchemeTypeList::fromValue);
    }

    @Nullable
    private static <T> T fromValue(String countryCode, Function<String, T> fromValue) {
        try {
            return fromValue.apply("N" + countryCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.info("Unknown country code.", e);
            return null;
        }
    }
}
