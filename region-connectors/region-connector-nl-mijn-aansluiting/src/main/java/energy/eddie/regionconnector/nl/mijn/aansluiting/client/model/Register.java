package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Register(
        @JsonProperty("Meter") Meter meter,
        @JsonProperty("MRID") String mrid,
        @JsonProperty("ReadingList") List<Reading> readingList
) {

    public void setReadingList(List<Reading> readingList) {
        this.readingList.clear();
        this.readingList.addAll(readingList);
    }
}

