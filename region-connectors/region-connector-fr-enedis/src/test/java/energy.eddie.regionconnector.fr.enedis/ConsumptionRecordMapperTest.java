package energy.eddie.regionconnector.fr.enedis;

import com.google.gson.Gson;
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
            Gson gson = new Gson();
            ConsumptionLoadCurveMeterReading consumptionLoadCurveMeterReading = gson.fromJson(content, ConsumptionLoadCurveMeterReading.class);
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
            Gson gson = new Gson();
            DailyConsumptionMeterReading dailyConsumptionMeterReading = gson.fromJson(content, DailyConsumptionMeterReading.class);
            System.out.println(dailyConsumptionMeterReading);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
