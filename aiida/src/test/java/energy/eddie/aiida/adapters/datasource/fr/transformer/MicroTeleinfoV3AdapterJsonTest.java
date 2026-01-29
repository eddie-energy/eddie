// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.fr.transformer;

import energy.eddie.aiida.adapters.datasource.fr.transformer.history.HistoryModeEntry;
import energy.eddie.aiida.adapters.datasource.fr.transformer.history.MicroTeleinfoV3HistoryModeJson;
import energy.eddie.aiida.config.AiidaConfiguration;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicroTeleinfoV3AdapterJsonTest {
    /**
     * Checks that the Jackson annotations are correct and every field of the JSON is deserialized as expected.
     */
    @Test
    void verify_isProperlyDeserialized() throws JacksonException {
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        ObjectMapper mapper = builder.build();

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

        var json = mapper.readValue(str, MicroTeleinfoV3HistoryModeJson.class);

        assertEquals("000000", json.energyData().get(HistoryModeEntry.MOTDETAT.toString()).raw());
        assertEquals("12345678901", json.energyData().get(HistoryModeEntry.ADCO.toString()).raw());
        assertEquals("HC..", json.energyData().get(HistoryModeEntry.OPTARIF.toString()).raw());
        assertEquals("45", json.energyData().get(HistoryModeEntry.ISOUSC.toString()).raw());
        assertEquals("905868888", json.energyData().get(HistoryModeEntry.BASE.toString()).raw());
        assertEquals("A", json.energyData().get(HistoryModeEntry.HHPHC.toString()).raw());
        assertEquals("HP..", json.energyData().get(HistoryModeEntry.PTEC.toString()).raw());
        assertEquals("22", json.energyData().get(HistoryModeEntry.IINST.toString()).raw());
        assertEquals("90", json.energyData().get(HistoryModeEntry.IMAX.toString()).raw());
        assertEquals("4110", json.energyData().get(HistoryModeEntry.PAPP.toString()).raw());
    }
}
