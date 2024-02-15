package energy.eddie.regionconnector.es.datadis.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeteringDataDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MeteringData.class, new MeteringDataDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void testNormalDateTimeDeserialization() throws Exception {
        String json = "{\"cups\":\"1\",\"date\":\"2024/02/08\",\"time\":\"15:00\",\"consumptionKWh\":1.0,\"obtainMethod\":\"REAL\",\"surplusEnergyKWh\":0.5}";
        MeteringData data = mapper.readValue(json, MeteringData.class);
        assertEquals(ZonedDateTime.of(2024, 2, 8, 15, 0, 0, 0, ZoneId.of("Europe/Madrid")), data.dateTime());
    }

    @Test
    void testDateIncrementWith2400Time() throws Exception {
        String json = "{\"cups\":\"1\",\"date\":\"2024/02/08\",\"time\":\"24:00\",\"consumptionKWh\":1.0,\"obtainMethod\":\"REAL\",\"surplusEnergyKWh\":0.5}";
        MeteringData data = mapper.readValue(json, MeteringData.class);
        assertEquals(ZonedDateTime.of(2024, 2, 9, 0, 0, 0, 0, ZoneId.of("Europe/Madrid")), data.dateTime());
    }

    @Test
    void testIncorrectDateFormat() {
        String json = "{\"cups\":\"1\",\"date\":\"08-02-2024\",\"time\":\"10:00\",\"consumptionKWh\":1.0,\"obtainMethod\":\"REAL\",\"surplusEnergyKWh\":0.5}";
        assertThrows(Exception.class, () -> mapper.readValue(json, MeteringData.class));
    }
}