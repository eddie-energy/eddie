// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ConnectionLimitCalculationTest {

    private static final UUID PERMISSION_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PERMISSION_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final String METER_1 = "meter-1";
    private static final String METER_2 = "meter-2";

    private static final String MRID_1 = "mrid-1";
    private static final String MRID_2 = "mrid-2";
    private static final String MRID_3 = "mrid-3";

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void calculatesEffectiveLimits(
            String name,
            List<ConnectionLimit> limits,
            List<ConnectionLimitDefault> defaults,
            Instant from,
            Instant to,
            List<ConnectionLimitDto> expected
    ) {
        var result = new ConnectionLimitCalculation(limits, defaults, from, to).effectiveLimits();

        assertThat(result).containsExactlyElementsOf(expected);
    }

    private static Stream<Arguments> scenarios() {
        return Stream.of(arguments("no limits and no default",
                                   limits(),
                                   defaults(),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected()),

                         arguments("single limit covers entire requested range",
                                   limits(limit("10:00-12:00", "08:00", 10, 100)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected(dto("10:00-12:00", 10, 100))),

                         arguments("single limit starts after requested range start",
                                   limits(limit("11:00-12:00", "08:00", 10, 100)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected(dto("11:00-12:00", 10, 100))),

                         arguments("single limit ends before requested range end",
                                   limits(limit("09:00-11:00", "08:00", 10, 100)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected(dto("10:00-11:00", 10, 100))),

                         arguments("single limit completely outside requested range",
                                   limits(limit("08:00-09:00", "08:00", 10, 100)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected()),

                         arguments("single limit ending at requested range start is excluded",
                                   limits(limit("09:00-10:00", "08:00", 10, 100)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected()),

                         arguments("single limit starting at requested range end is excluded",
                                   limits(limit("12:00-13:00", "08:00", 10, 100)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected()),

                         arguments("adjacent limits",
                                   limits(limit("10:00-11:00", MRID_1, "08:00", 10, 100),
                                          limit("11:00-12:00", MRID_2, "09:00", 20, 80)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected(dto("10:00-11:00", MRID_1, 10, 100), dto("11:00-12:00", MRID_2, 20, 80))),

                         arguments("newer limit completely overrides older limit on overlap",
                                   limits(limit("10:00-14:00", MRID_1, "08:00", 10, 100),
                                          limit("11:00-13:00", MRID_2, "09:00", 20, 80)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("14:00"),
                                   expected(dto("10:00-11:00", MRID_1, 10, 100),
                                            dto("11:00-13:00", MRID_2, 20, 80),
                                            dto("13:00-14:00", MRID_1, 10, 100))),

                         arguments("newer limit starts before older limit ends",
                                   limits(limit("10:00-12:00", MRID_1, "08:00", 10, 100),
                                          limit("11:00-14:00", MRID_2, "09:00", 20, 80)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("14:00"),
                                   expected(dto("10:00-11:00", MRID_1, 10, 100), dto("11:00-14:00", MRID_2, 20, 80))),

                         arguments("newer limit ends before older limit ends",
                                   limits(limit("10:00-14:00", MRID_1, "08:00", 10, 100),
                                          limit("11:00-13:00", MRID_2, "09:00", 20, 80)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("14:00"),
                                   expected(dto("10:00-11:00", MRID_1, 10, 100),
                                            dto("11:00-13:00", MRID_2, 20, 80),
                                            dto("13:00-14:00", MRID_1, 10, 100))),

                         arguments("multiple overlapping limits select newest active limit",
                                   limits(limit("10:00-16:00", MRID_1, "08:00", 10, 100),
                                          limit("11:00-15:00", MRID_2, "09:00", 20, 80),
                                          limit("12:00-14:00", MRID_3, "10:00", 30, 70)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("16:00"),
                                   expected(dto("10:00-11:00", MRID_1, 10, 100),
                                            dto("11:00-12:00", MRID_2, 20, 80),
                                            dto("12:00-14:00", MRID_3, 30, 70),
                                            dto("14:00-15:00", MRID_2, 20, 80),
                                            dto("15:00-16:00", MRID_1, 10, 100))),

                         arguments("last inserted limit wins when overlapping limits have equal createdAt",
                                   limits(limit("10:00-14:00", MRID_1, "08:00", 10, 100),
                                          limit("11:00-13:00", MRID_2, "08:00", 20, 80)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("14:00"),
                                   expected(dto("10:00-11:00", MRID_1, 10, 100),
                                            dto("11:00-13:00", MRID_2, 20, 80),
                                            dto("13:00-14:00", MRID_1, 10, 100))),

                         arguments("default fills entire range when no limits exist",
                                   limits(),
                                   defaults(defaultLimit(5, 50)),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected(defaultDto("10:00-12:00", 5, 50))),

                         arguments("default fills gaps around a connection limit",
                                   limits(limit("11:00-12:00", "08:00", 10, 100)),
                                   defaults(defaultLimit(5, 50)),
                                   instant("10:00"),
                                   instant("13:00"),
                                   expected(defaultDto("10:00-11:00", 5, 50),
                                            dto("11:00-12:00", 10, 100),
                                            defaultDto("12:00-13:00", 5, 50))),

                         arguments("default fills gap between two limits",
                                   limits(limit("11:00-12:00", MRID_1, "08:00", 10, 100),
                                          limit("13:00-14:00", MRID_2, "09:00", 20, 80)),
                                   defaults(defaultLimit(PERMISSION_1, METER_1, 5, 50)),
                                   instant("10:00"),
                                   instant("15:00"),
                                   expected(defaultDto(PERMISSION_1, METER_1, "10:00-11:00", 5, 50),
                                            dto("11:00-12:00", MRID_1, 10, 100),
                                            defaultDto(PERMISSION_1, METER_1, "12:00-13:00", 5, 50),
                                            dto("13:00-14:00", MRID_2, 20, 80),
                                            defaultDto(PERMISSION_1, METER_1, "14:00-15:00", 5, 50))),

                         arguments("no default leaves uncovered periods empty",
                                   limits(limit("11:00-12:00", "08:00", 10, 100)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("13:00"),
                                   expected(dto("11:00-12:00", 10, 100))),

                         arguments("different permissions are calculated independently",
                                   limits(limit(PERMISSION_1, METER_1, "10:00-13:00", MRID_1, "08:00", 10, 100),
                                          limit(PERMISSION_2, METER_1, "11:00-12:00", MRID_2, "09:00", 20, 80)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("13:00"),
                                   expected(dto(PERMISSION_1, METER_1, "10:00-13:00", MRID_1, 10, 100),
                                            dto(PERMISSION_2, METER_1, "11:00-12:00", MRID_2, 20, 80))),

                         arguments("different meters are calculated independently",
                                   limits(limit(PERMISSION_1, METER_1, "10:00-13:00", MRID_1, "08:00", 10, 100),
                                          limit(PERMISSION_1, METER_2, "11:00-12:00", MRID_2, "09:00", 20, 80)),
                                   defaults(),
                                   instant("10:00"),
                                   instant("13:00"),
                                   expected(dto(PERMISSION_1, METER_1, "10:00-13:00", MRID_1, 10, 100),
                                            dto(PERMISSION_1, METER_2, "11:00-12:00", MRID_2, 20, 80))),

                         arguments("null default meter id is represented as empty meter id",
                                   limits(),
                                   defaults(defaultLimit(PERMISSION_1, null, 5, 50)),
                                   instant("10:00"),
                                   instant("12:00"),
                                   expected(defaultDto(PERMISSION_1, "", "10:00-12:00", 5, 50))));
    }

    private static List<ConnectionLimit> limits(ConnectionLimit... values) {
        return List.of(values);
    }

    private static List<ConnectionLimitDefault> defaults(ConnectionLimitDefault... values) {
        return List.of(values);
    }

    private static List<ConnectionLimitDto> expected(ConnectionLimitDto... values) {
        return List.of(values);
    }

    private static ConnectionLimit limit(String interval, String createdAt, int min, int max) {
        return limit(interval, MRID_1, createdAt, min, max);
    }

    private static ConnectionLimit limit(String interval, String mrid, String createdAt, int min, int max) {
        return limit(PERMISSION_1, METER_1, interval, mrid, createdAt, min, max);
    }

    private static ConnectionLimit limit(
            UUID permissionId,
            String meterId,
            String interval,
            String mrid,
            String createdAt,
            int min,
            int max
    ) {
        var times = interval.split("-");

        return new ConnectionLimit(permissionId,
                                   meterId,
                                   instant(times[0]),
                                   instant(times[1]),
                                   BigDecimal.valueOf(min),
                                   BigDecimal.valueOf(max),
                                   mrid,
                                   1,
                                   instant(createdAt));
    }

    private static ConnectionLimitDefault defaultLimit(int min, int max) {
        return defaultLimit(PERMISSION_1, METER_1, min, max);
    }

    private static ConnectionLimitDefault defaultLimit(UUID permissionId, String meterId, int min, int max) {
        return new ConnectionLimitDefault(permissionId, meterId, BigDecimal.valueOf(min), BigDecimal.valueOf(max));
    }

    private static ConnectionLimitDto dto(String interval, int min, int max) {
        return dto(interval, MRID_1, min, max);
    }

    private static ConnectionLimitDto dto(String interval, String mrid, int min, int max) {
        return dto(PERMISSION_1, METER_1, interval, mrid, min, max);
    }

    private static ConnectionLimitDto dto(
            UUID permissionId,
            String meterId,
            String interval,
            String mrid,
            int min,
            int max
    ) {
        var times = interval.split("-");

        return new ConnectionLimitDto(permissionId,
                                      meterId,
                                      mrid,
                                      instant(times[0]),
                                      instant(times[1]),
                                      BigDecimal.valueOf(min),
                                      BigDecimal.valueOf(max));
    }

    private static ConnectionLimitDto defaultDto(String interval, int min, int max) {
        return defaultDto(PERMISSION_1, METER_1, interval, min, max);
    }

    private static ConnectionLimitDto defaultDto(UUID permissionId, String meterId, String interval, int min, int max) {
        return dto(permissionId, meterId, interval, null, min, max);
    }

    private static Instant instant(String time) {
        return Instant.parse("2026-01-01T" + time + ":00Z");
    }
}