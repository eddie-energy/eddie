package energy.eddie.regionconnector.shared.cim.v0_82;

import energy.eddie.cim.v0_82.pmd.CodingSchemeTypeList;
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
    public static CodingSchemeTypeList getCodingSchemePmd(String countryCode) {
        return fromValue(countryCode, energy.eddie.cim.v0_82.pmd.CodingSchemeTypeList::fromValue);
    }

    @Nullable
    private static <T> T fromValue(String countryCode, Function<String, T> fromValue) {
        try {
            return fromValue.apply("N" + countryCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // prevent exception spamming until GH-638 is implemented
            if (!countryCode.equalsIgnoreCase("aiida"))
                LOGGER.info("Unknown country code.", e);
            return null;
        }
    }

    @Nullable
    public static energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList getCodingSchemeVhd(String countryCode) {
        return fromValue(countryCode, energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList::fromValue);
    }

    @Nullable
    public static energy.eddie.cim.v0_82.ap.CodingSchemeTypeList getCodingSchemeAp(String countryCode) {
        return fromValue(countryCode, energy.eddie.cim.v0_82.ap.CodingSchemeTypeList::fromValue);
    }
}
