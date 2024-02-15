package energy.eddie.regionconnector.es.datadis.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class MeteringDataSerializer extends JsonSerializer<MeteringData> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void serialize(MeteringData value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("cups", value.cups());
        gen.writeStringField("date", value.dateTime().toLocalDate().format(DATE_FORMAT));
        gen.writeStringField("time", value.dateTime().toLocalTime().format(TIME_FORMAT));
        gen.writeNumberField("consumptionKWh", value.consumptionKWh());
        gen.writeStringField("obtainMethod", value.obtainMethod());
        gen.writeNumberField("surplusEnergyKWh", value.surplusEnergyKWh());
        gen.writeEndObject();
    }
}