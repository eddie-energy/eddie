package energy.eddie.regionconnector.dk.energinet.utils;

import io.javalin.validation.JavalinValidation;
import jakarta.annotation.Nullable;
import kotlin.jvm.functions.Function1;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ZonedDateTimeConverter implements Function1<String, ZonedDateTime> {

    public static void register() {
        JavalinValidation.register(ZonedDateTime.class, new ZonedDateTimeConverter());
    }

    @Nullable
    @Override
    public ZonedDateTime invoke(String value) {
        try {
            return value != null && !value.isBlank() ? LocalDate.parse(value, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneOffset.UTC) : null;
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }
}
