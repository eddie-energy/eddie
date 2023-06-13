package energy.eddie.regionconnector.fr.enedis.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionMeterReading;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class ConsumptionRecordMapperTest {

    @Test
    public void testCorrectClcMapping() {
        var classLoader = getClass().getClassLoader();
        var url = classLoader.getResource("consumption-load-curve-record.json");

        try {
            String content = Files.readString(Paths.get(Objects.requireNonNull(url).getFile()));
            ObjectMapper objectMapper = new ObjectMapper();
            ConsumptionLoadCurveMeterReading consumptionLoadCurveMeterReading = objectMapper.readValue(content, ConsumptionLoadCurveMeterReading.class);
            System.out.println(consumptionLoadCurveMeterReading);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCorrectDcMapping() {
        var classLoader = getClass().getClassLoader();
        var url = classLoader.getResource("daily-consumption-record.json");

        try {
            String content = Files.readString(Paths.get(Objects.requireNonNull(url).getFile()));
            ObjectMapper objectMapper = new ObjectMapper();
            DailyConsumptionMeterReading dailyConsumptionMeterReading = objectMapper.readValue(content, DailyConsumptionMeterReading.class);
            System.out.println(dailyConsumptionMeterReading);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
