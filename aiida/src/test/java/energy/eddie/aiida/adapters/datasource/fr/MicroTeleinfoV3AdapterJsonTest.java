package energy.eddie.aiida.adapters.datasource.fr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import energy.eddie.aiida.config.AiidaConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicroTeleinfoV3AdapterJsonTest {
    /**
     * Checks that the Jackson annotations are correct and every field of the JSON is deserialized as expected.
     */
    @Test
    void verify_isProperlyDeserialized() throws JsonProcessingException {
        ObjectMapper mapper = new AiidaConfiguration().objectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(MicroTeleinfoV3AdapterJson.TeleinfoDataField.class,
                               new MicroTeleinfoV3AdapterValueDeserializer(null));
        mapper.registerModule(module);


        String str = """
                {
                     "MOTDETAT": {
                         "raw": "000000",
                         "value": 0
                     },
                     "ADCO": {
                         "raw": "12345678901",
                         "value": 12345678901
                     },
                     "OPTARIF": {
                         "raw": "HC..",
                         "value": "HC"
                     },
                     "ISOUSC": {
                         "raw": "45",
                         "value": 45
                     },
                     "BASE": {
                         "raw": "905868888",
                         "value": 905868888
                     },
                     "HHPHC": {
                         "raw": "A",
                         "value": "A"
                     },
                     "PTEC": {
                         "raw": "HP..",
                         "value": "HP"
                     },
                     "IINST": {
                         "raw": "22",
                         "value": 22
                     },
                     "IMAX": {
                         "raw": "90",
                         "value": 90
                     },
                     "PAPP": {
                         "raw": "4110",
                         "value": 4110
                     }
                 }
                
                """;

        var json = mapper.readValue(str, MicroTeleinfoV3AdapterJson.class);

        assertEquals("000000", json.motdetat().raw());
        assertEquals("12345678901", json.adco().raw());
        assertEquals("HC..", json.optarif().raw());
        assertEquals("45", json.isousc().raw());
        assertEquals("905868888", json.base().raw());
        assertEquals("A", json.hhphc().raw());
        assertEquals("HP..", json.ptec().raw());
        assertEquals("22", json.iinst().raw());
        assertEquals("90", json.imax().raw());
        assertEquals("4110", json.papp().raw());
    }
}
