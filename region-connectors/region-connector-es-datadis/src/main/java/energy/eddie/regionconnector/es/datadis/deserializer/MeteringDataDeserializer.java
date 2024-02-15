package energy.eddie.regionconnector.es.datadis.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

public class MeteringDataDeserializer extends JsonDeserializer<MeteringData> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public MeteringData deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        String cups = node.path("cups").asText();
        String dateString = node.path("date").asText();
        String timeString = node.path("time").asText();
        Double consumptionKWh = node.path("consumptionKWh").asDouble(0);
        String obtainMethod = node.path("obtainMethod").asText();
        Double surplusEnergyKWh = node.path("surplusEnergyKWh").asDouble(0);

        LocalDate date = LocalDate.parse(dateString, DATE_FORMAT);
        LocalTime time = LocalTime.parse(timeString, TIME_FORMAT);

        // Datadis API returns the time 24:00 for the next day, this is parsed as LocalTime.MIN, but we need to add a day to the date
        if (time.equals(LocalTime.MIN)) {
            date = date.plusDays(1);
        }

        return new MeteringData(cups, ZonedDateTime.of(date, time, ZONE_ID_SPAIN), consumptionKWh, obtainMethod, surplusEnergyKWh);
    }
}