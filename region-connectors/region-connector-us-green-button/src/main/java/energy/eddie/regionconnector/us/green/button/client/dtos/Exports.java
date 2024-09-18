package energy.eddie.regionconnector.us.green.button.client.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

@SuppressWarnings("unused")
public class Exports {
    @Nullable
    @JsonProperty(value = "bills_csv")
    private final String billsCsv;
    @Nullable
    @JsonProperty(value = "bills_zip")
    private final String billsZip;
    @Nullable
    @JsonProperty(value = "latest_pdf")
    private final String latestPdf;
    @JsonProperty(value = "intervals_csv")
    @Nullable
    private final String intervalsCsv;
    @Nullable
    @JsonProperty(value = "intervals_xml")
    private final String intervalsXml;

    @JsonCreator
    public Exports(
            @Nullable String billsCsv,
            @Nullable String billsZip,
            @Nullable String latestPdf,
            @Nullable String intervalsCsv,
            @Nullable String intervalsXml
    ) {
        this.billsCsv = billsCsv;
        this.billsZip = billsZip;
        this.latestPdf = latestPdf;
        this.intervalsCsv = intervalsCsv;
        this.intervalsXml = intervalsXml;
    }
}
