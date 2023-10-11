package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import io.javalin.validation.JavalinValidation;
import jakarta.annotation.Nullable;
import kotlin.jvm.functions.Function1;

public class PeriodResolutionEnumConverter implements Function1<String, PeriodResolutionEnum> {

    public static void register() {
        JavalinValidation.register(PeriodResolutionEnum.class, new PeriodResolutionEnumConverter());
    }

    @Nullable
    @Override
    public PeriodResolutionEnum invoke(String value) {
        try {
            return value != null && !value.isBlank() ? PeriodResolutionEnum.fromString(value) : null;
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }
}