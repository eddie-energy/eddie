package energy.eddie.regionconnector.us.green.button.permission.request.helper;

import energy.eddie.api.agnostic.Granularity;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SuppressWarnings("NullAway")
public class Scope {
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd"; // 2024-05-09
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);

    private final ScopeBuilder builder;

    private Scope() {
        throw new UnsupportedOperationException("Scope cannot be created without a builder.");
    }

    private Scope(ScopeBuilder builder) {this.builder = builder;}

    @Override
    public String toString() {
        return generateScopeRepresentation();
    }

    private String generateScopeRepresentation() {
        var scopeStringBuilder = new StringBuilder("FB=");

        // Requesting data fields
        // Examples:
        //  - FB=4
        //  - FB=16
        //  - FB=4_16
        //  - FB=4_16_51
        var dataFields = builder.dataFields();
        dataFields.forEach(dataField -> scopeStringBuilder.append(dataField.getCode()).append("_"));
        if (!dataFields.isEmpty()) {
            scopeStringBuilder.deleteCharAt(scopeStringBuilder.length() - 1);
        }

        // Interval duration given in seconds
        scopeStringBuilder.append(";IntervalDuration=").append(builder.granularity().minutes() * 60);

        // Additional scope such as historical data start, ongoing data end
        scopeStringBuilder.append(";AdditionalScope=historical-");

        // Historical data
        // Examples:
        //  - FB=4;AdditionalScope=historical-2024-05-09_ongoing-none_meters-all_sou-default_noedit
        //  - FB=4;AdditionalScope=historical-2024-05-09_ongoing-2024-05-23_meters-all_sou-default_noedit
        var historicalDataStart = builder.historicalDataStart();
        if (historicalDataStart.isEmpty()) {
            scopeStringBuilder.append("none");
        } else {
            // Representation example of date after format: 2024-05-09
            scopeStringBuilder.append(historicalDataStart.get().format(dateFormatter));
        }

        // Ongoing data
        // Examples:
        //  - FB=4;AdditionalScope=historical-none_ongoing-2024-05-23_meters-all_sou-default_noedit
        //  - FB=4;AdditionalScope=historical-2024-05-09_ongoing-2024-05-23_meters-all_sou-default_noedit
        scopeStringBuilder.append("_ongoing-");
        var ongoingDataEnd = builder.ongoingDataEnd();
        if (ongoingDataEnd.isEmpty()) {
            scopeStringBuilder.append("none");
        } else {
            // Representation example of date after format: 2024-05-23
            scopeStringBuilder.append(ongoingDataEnd.get().format(dateFormatter));
        }

        scopeStringBuilder.append("_sou-default_noedit");
        return scopeStringBuilder.toString();
    }

    public enum DataField {
        ACCOUNT_DETAILS(51), BILLS(16), INTERVALS(4);

        private final int code;

        DataField(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public static class ScopeBuilder {
        private Set<DataField> dataFields = new TreeSet<>(Comparator.comparing(DataField::getCode));
        private LocalDate historicalDataStart;
        private LocalDate ongoingDataEnd;
        private Granularity granularity;

        public ScopeBuilder dataFields(Set<DataField> dataFields) {
            this.dataFields = dataFields;
            return this;
        }

        public ScopeBuilder addDataField(DataField dataField) {
            this.dataFields.add(dataField);
            return this;
        }

        public ScopeBuilder withHistoricalDataStart(LocalDate historicalDataStart) {
            this.historicalDataStart = historicalDataStart;
            return this;
        }

        public ScopeBuilder withOngoingDataEnd(LocalDate ongoingDataEnd) {
            this.ongoingDataEnd = ongoingDataEnd;
            return this;
        }

        public ScopeBuilder withGranularity(Granularity granularity) {
            this.granularity = granularity;
            return this;
        }

        public Scope build() {
            Objects.requireNonNull(granularity);

            if (dataFields.isEmpty()) {
                throw new IllegalStateException("At least one data field must be set.");
            }

            if (historicalDataStart == null && ongoingDataEnd == null) {
                throw new IllegalStateException("At least historicalDataStart or ongoingDataEnd must be set.");
            }

            completelyInThePastOrFuture();
            return new Scope(this);
        }

        private void completelyInThePastOrFuture() {
            if (historicalDataStart != null && ongoingDataEnd != null) {
                var now = LocalDate.now(ZoneOffset.UTC);
                if (historicalDataStart.isBefore(now) && ongoingDataEnd.isBefore(now)) {
                    ongoingDataEnd = null;
                } else if (historicalDataStart.isAfter(now) && ongoingDataEnd.isAfter(now)) {
                    historicalDataStart = null;
                }
            }
        }

        public Set<DataField> dataFields() {
            return dataFields;
        }

        public Optional<LocalDate> historicalDataStart() {
            return Optional.ofNullable(historicalDataStart);
        }

        public Optional<LocalDate> ongoingDataEnd() {
            return Optional.ofNullable(ongoingDataEnd);
        }

        public Granularity granularity() {
            return granularity;
        }
    }
}
