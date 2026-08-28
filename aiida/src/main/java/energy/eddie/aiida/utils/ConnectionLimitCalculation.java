// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.utils;

import energy.eddie.aiida.dtos.connectionlimit.ConnectionLimitDto;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitDefault;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * Provides {@link #effectiveLimits()} to calculate which limits apply when during the requested time frame and scope.
 * <p>
 * Limits are scoped by permission id and meter id and may overlap within the same scope.
 * When multiple limits overlap, the limit with the newest createdAt timestamp takes precedence.
 * <p>
 * The calculation is performed independently for each scope using a sweep-line algorithm:
 * <ol>
 *   <li>Create start and end events for all limits.</li>
 *   <li>Sweep chronologically through the requested interval.</li>
 *   <li>Maintain the currently active limits per event.</li>
 *   <li>Select the active limit with the newest createdAt.</li>
 *   <li>Emit effective time segments.</li>
 * </ol>
 * <p>
 * If no limit is active for a segment, the scope default is used when available.
 * If neither limits nor a default exist for a scope, no segment is emitted.
 * <p>
 * If multiple active limits share the same newest creation timestamp, the most recently inserted is used.
 */
public final class ConnectionLimitCalculation {
    private final List<ConnectionLimit> limits;
    private final List<ConnectionLimitDefault> defaults;
    private final Instant from;
    private final Instant to;

    public ConnectionLimitCalculation(
            List<ConnectionLimit> limits,
            List<ConnectionLimitDefault> defaults,
            Instant from,
            Instant to
    ) {
        this.limits = limits;
        this.defaults = defaults;
        this.from = from;
        this.to = to;
    }

    public List<ConnectionLimitDto> effectiveLimits() {
        var scopedLimits = new HashMap<Scope, List<ConnectionLimit>>();
        for (ConnectionLimit limit : limits) {
            var scope = new Scope(limit.permissionId(), limit.meterId());
            scopedLimits.computeIfAbsent(scope, ignored -> new ArrayList<>()).add(limit);
        }

        var scopedDefaults = new HashMap<Scope, ConnectionLimitDefault>();
        for (ConnectionLimitDefault limitDefault : defaults) {
            var scope = new Scope(limitDefault.permissionId(), Objects.requireNonNullElse(limitDefault.meterId(), ""));
            scopedDefaults.put(scope, limitDefault);
        }

        var scopes = new LinkedHashSet<Scope>();
        scopes.addAll(scopedLimits.keySet());
        scopes.addAll(scopedDefaults.keySet());

        var effectiveLimits = new ArrayList<ConnectionLimitDto>();
        for (var scope : scopes) {
            effectiveLimits.addAll(calculateScoped(scope, scopedLimits.get(scope), scopedDefaults.get(scope)));
        }

        return effectiveLimits.stream()
                              .sorted(Comparator.comparing(ConnectionLimitDto::permissionId)
                                                .thenComparing(ConnectionLimitDto::meterId)
                                                .thenComparing(ConnectionLimitDto::intervalStart))
                              .toList();
    }

    private List<ConnectionLimitDto> calculateScoped(
            Scope scope,
            @Nullable List<ConnectionLimit> limits,
            @Nullable ConnectionLimitDefault defaultLimit
    ) {
        if (limits == null || limits.isEmpty()) {
            return defaultLimit == null ? List.of() : List.of(defaultDto(scope, from, to, defaultLimit));
        }

        var result = new ArrayList<ConnectionLimitDto>();
        var sweep = new Sweep(limits, from, to);

        while (sweep.hasNext()) {
            var segment = sweep.next();

            if (segment.limit() != null) {
                result.add(limitDto(scope, segment.from(), segment.to(), segment.limit()));
            } else if (defaultLimit != null) {
                result.add(defaultDto(scope, segment.from(), segment.to(), defaultLimit));
            }
        }

        return result;
    }

    private ConnectionLimitDto defaultDto(Scope scope, Instant from, Instant to, ConnectionLimitDefault defaultLimit) {
        return new ConnectionLimitDto(scope.permissionId(),
                                      scope.meterId(),
                                      null,
                                      from,
                                      to,
                                      defaultLimit.minLimitKw(),
                                      defaultLimit.maxLimitKw());
    }

    private ConnectionLimitDto limitDto(Scope scope, Instant from, Instant to, ConnectionLimit connectionLimit) {
        return new ConnectionLimitDto(scope.permissionId(),
                                      scope.meterId(),
                                      connectionLimit.mrid(),
                                      from,
                                      to,
                                      connectionLimit.minLimitKw(),
                                      connectionLimit.maxLimitKw());
    }

    private record Scope(UUID permissionId, String meterId) {}

    /**
     * Internal sweep-line implementation producing segments of most recent connection limits.
     */
    private static final class Sweep {

        private final List<Event> events;
        private final ActiveLimits active;
        private final Instant to;
        private Instant current;
        private int eventIndex;

        Sweep(List<ConnectionLimit> limits, Instant from, Instant to) {
            this.to = to;
            this.current = from;
            this.eventIndex = 0;

            this.events = new ArrayList<>();
            this.active = new ActiveLimits();

            for (var limit : limits) {
                if (limit.intervalStart().isAfter(from) && limit.intervalStart().isBefore(to)) {
                    events.add(new Event(limit.intervalStart(), Event.EventType.START, limit));
                }

                if (limit.intervalEnd().isAfter(from) && limit.intervalEnd().isBefore(to)) {
                    events.add(new Event(limit.intervalEnd(), Event.EventType.END, limit));
                }

                if (!limit.intervalStart().isAfter(from) && limit.intervalEnd().isAfter(from)) {
                    active.add(limit);
                }
            }

            events.sort(Comparator.comparing(Event::instant));
        }

        boolean hasNext() {
            return current.isBefore(to);
        }

        Segment next() {
            var start = current;
            var winner = active.winner();

            while (current.isBefore(to)) {
                current = eventIndex < events.size() ? events.get(eventIndex).instant() : to;

                while (eventIndex < events.size() && events.get(eventIndex).instant().equals(current)) {
                    var event = events.get(eventIndex);

                    switch (event.type()) {
                        case START -> active.add(event.limit());
                        case END -> active.remove(event.limit());
                    }

                    eventIndex++;
                }

                var nextWinner = active.winner();

                // intentionally compare by reference
                if (winner != nextWinner) {
                    return new Segment(start, current, winner);
                }
            }

            return new Segment(start, to, winner);
        }

        private record Segment(Instant from, Instant to, @Nullable ConnectionLimit limit) {}

        private record Event(Instant instant, EventType type, ConnectionLimit limit) {
            private enum EventType {START, END}
        }

        /**
         * Tracks the limits that can apply at the sweep line position.
         * Ordered by descending createdAt timestamp to determine the effective limit in O(log n).
         */
        private static final class ActiveLimits {

            private final TreeMap<Instant, LinkedHashSet<ConnectionLimit>> limits = new TreeMap<>(Comparator.reverseOrder());

            void add(ConnectionLimit limit) {
                limits.computeIfAbsent(limit.createdAt(), ignored -> new LinkedHashSet<>()).add(limit);
            }

            void remove(ConnectionLimit limit) {
                var bucket = limits.get(limit.createdAt());

                if (bucket == null) {
                    return;
                }

                bucket.remove(limit);

                if (bucket.isEmpty()) {
                    limits.remove(limit.createdAt());
                }
            }

            @Nullable ConnectionLimit winner() {
                var newest = limits.firstEntry();

                if (newest == null) {
                    return null;
                }

                return newest.getValue().getLast();
            }
        }
    }
}
