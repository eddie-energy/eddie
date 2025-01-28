package energy.eddie.regionconnector.us.green.button.client.dtos.meter;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Meter {
    @JsonProperty(required = true)
    private final String uid;
    @JsonProperty(value = "authorization_uid", required = true)
    private final String authorizationUid;
    @JsonProperty(required = true)
    private final ZonedDateTime created;
    @JsonProperty(value = "user_email", required = true)
    private final String userEmail;
    @JsonProperty(value = "user_uid", required = true)
    private final String userUid;
    @JsonProperty(value = "is_archived", required = true)
    private final boolean isArchived;
    @JsonProperty(value = "is_activated", required = true)
    private final boolean isActivated;
    @JsonProperty(value = "is_expanded", required = true)
    private final boolean isExpanded;
    @JsonProperty(required = true)
    private final List<MeterNote> notes;
    @JsonProperty(required = true)
    private final String status;
    @JsonProperty(value = "status_message", required = true)
    private final String statusMessage;
    @JsonProperty(value = "status_ts", required = true)
    private final ZonedDateTime statusTS;
    @JsonProperty(value = "ongoing_monitoring", required = true)
    private final OngoingMonitoring ongoingMonitoring;
    @JsonProperty(required = true)
    private final String utility;
    @JsonProperty(value = "bill_count", required = true)
    private final int billCount;
    @JsonProperty(value = "bill_coverage", required = true)
    private final List<List<ZonedDateTime>> billCoverage;
    @JsonProperty(value = "bill_sources", required = true)
    private final List<String> billSources;
    @JsonProperty(value = "interval_count", required = true)
    private final int intervalCount;
    @JsonProperty(value = "interval_coverage", required = true)
    private final List<List<ZonedDateTime>> intervalCoverage;
    @JsonProperty(value = "interval_sources", required = true)
    private final List<String> intervalSources;
    @JsonProperty(required = true)
    private final Exports exports;
    @JsonProperty(value = "exports_list", required = true)
    private final List<Export> exportList;
    @JsonProperty(value = "blocks", required = true)
    private final List<String> blocks;
    @JsonProperty(value = "other_devices")
    private final List<Device> otherDevices;
    @JsonProperty(value = "programs")
    private final List<Program> programs;
    @JsonProperty(value = "suppliers")
    private final List<Supplier> suppliers;
    @JsonProperty
    private final Map<String, MeterBlock> meterBlocks = new HashMap<>();

    @JsonCreator
    public Meter(
            String uid,
            String authorizationUid,
            ZonedDateTime created,
            String userEmail,
            String userUid,
            boolean isArchived,
            boolean isActivated,
            boolean isExpanded,
            List<MeterNote> notes,
            String status,
            String statusMessage,
            ZonedDateTime statusTS,
            OngoingMonitoring ongoingMonitoring,
            String utility,
            int billCount,
            List<List<ZonedDateTime>> billCoverage,
            List<String> billSources,
            int intervalCount,
            List<List<ZonedDateTime>> intervalCoverage,
            List<String> intervalSources,
            Exports exports,
            List<Export> exportList,
            List<String> blocks,
            List<Device> otherDevices,
            List<Program> programs,
            List<Supplier> suppliers
    ) {
        this.uid = uid;
        this.authorizationUid = authorizationUid;
        this.created = created;
        this.userEmail = userEmail;
        this.userUid = userUid;
        this.isArchived = isArchived;
        this.isActivated = isActivated;
        this.isExpanded = isExpanded;
        this.notes = notes;
        this.status = status;
        this.statusMessage = statusMessage;
        this.statusTS = statusTS;
        this.ongoingMonitoring = ongoingMonitoring;
        this.utility = utility;
        this.billCount = billCount;
        this.billCoverage = billCoverage;
        this.billSources = billSources;
        this.intervalCount = intervalCount;
        this.intervalCoverage = intervalCoverage;
        this.intervalSources = intervalSources;
        this.exports = exports;
        this.exportList = exportList;
        this.blocks = blocks;
        this.otherDevices = otherDevices;
        this.programs = programs;
        this.suppliers = suppliers;
    }

    public String uid() {
        return uid;
    }

    public String authorizationUid() {
        return authorizationUid;
    }

    public Map<String, MeterBlock> meterBlocks() {
        return meterBlocks;
    }

    @JsonAnySetter
    public void setMeterBlock(String key, MeterBlock value) {
        meterBlocks.put(key, value);
    }
}
