// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers;

import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.DateAndOrTime;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Reading;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.ReadingType;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.model.Register;

import java.util.*;

/**
 * Prepares the registers of a metered data response, independent of the CIM version they are mapped to.
 * Cumulative readings are converted to deltas and registers of the same meter and OBIS code are summed up.
 */
public final class MeteredDataRegisters {
    private MeteredDataRegisters() {
        // Utility Class
    }

    public static List<Register> sumUpRegistersWithSameObisCode(List<Register> registers) {
        var partitions = partitionRegistersByMeter(registers);
        var partitionedByMeterAndObisCode = partitionByObisCode(partitions);
        return partitionedByMeterAndObisCode.stream()
                                            .map(partition -> partition.stream()
                                                                       .reduce(MeteredDataRegisters::mergeAndSum))
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
                                            .toList();
    }

    private static List<List<Register>> partitionByObisCode(List<List<Register>> partitions) {
        var partitionedByMeterAndObisCode = new ArrayList<List<Register>>();
        for (var partition : partitions) {
            var map = new HashMap<String, List<Register>>();
            for (var register : partition) {
                var obisCode = register.mrid();
                var code = switch (obisCode.subSequence(0, 3).toString()) {
                    case "1.8" -> "1.8";
                    case "2.8" -> "2.8";
                    default -> obisCode;
                };
                var res = map.getOrDefault(code, new ArrayList<>());
                res.add(register);
                map.put(code, res);
            }
            partitionedByMeterAndObisCode.addAll(map.values());
        }
        return partitionedByMeterAndObisCode;
    }

    private static Register mergeAndSum(Register lr, Register rr) {
        Map<DateAndOrTime, Reading> map = new HashMap<>();

        for (var reading : delta(lr.readingList())) {
            map.put(reading.dateAndOrTime(),
                    new Reading(reading.dateAndOrTime(), reading.readingType(), reading.value()));
        }

        for (var reading : delta(rr.readingList())) {
            var dateAndOrTime = reading.dateAndOrTime();
            map.merge(dateAndOrTime, reading,
                      (existing, incoming) -> new Reading(dateAndOrTime,
                                                          existing.readingType(),
                                                          existing.value().add(incoming.value()))
            );
        }

        var readingList = new ArrayList<>(map.values());
        readingList.sort(Comparator.comparing(r -> r.dateAndOrTime().dateTime()));
        return new Register(lr.meter(), lr.mrid(), readingList);
    }

    private static List<List<Register>> partitionRegistersByMeter(List<Register> registers) {
        var map = new HashMap<String, List<Register>>();
        for (var register : registers) {
            var mrid = register.meter().mrid();
            var res = map.getOrDefault(mrid, new ArrayList<>());
            res.add(register);
            map.put(mrid, res);
        }
        return new ArrayList<>(map.values());
    }

    public static String readingUnit(ReadingType readingType) {
        var multiplier = (readingType.multiplier() == null ? "" : readingType.multiplier().toString());
        var readingUnit = multiplier + readingType.unit();
        return readingUnit.toUpperCase(Locale.ROOT);
    }

    /**
     * Gets a reading list and will calculate the deltas between consecutive items in the list.
     * By calculating deltas, the first item will be dropped.
     *
     * @param readings list of readings with total cumulative values.
     * @return list of readings, with the delta as reading values. It has the size of the original list - 1.
     */
    private static List<Reading> delta(List<Reading> readings) {
        var array = readings.toArray(new Reading[0]);
        List<Reading> deltas = new ArrayList<>(array.length - 1);
        for (int i = array.length - 1; i > 0; i--) {
            var prev = array[i - 1];
            var current = array[i];
            var delta = current.value().subtract(prev.value());
            deltas.addFirst(new Reading(current.dateAndOrTime(), current.readingType(), delta));
        }
        return deltas;
    }
}
