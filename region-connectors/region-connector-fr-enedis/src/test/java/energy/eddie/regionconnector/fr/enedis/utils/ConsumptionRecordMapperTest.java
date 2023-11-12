package energy.eddie.regionconnector.fr.enedis.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionReadingType;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumptionRecordMapperTest {

    private static final String USAGE_POINT_ID = "11453290002823";
    private static final String START_DATE = "2023-05-29";
    private static final String END_DATE = "2023-05-30";

    private String readClasspathResource(String path) {
        var inputStream = Objects.requireNonNull(getClass().getResourceAsStream(path));
        return new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset())).lines().collect(Collectors.joining("\n"));
    }

    @Test
    void testCorrectClcMapping() throws Exception {
        String content = readClasspathResource("/consumption-load-curve-record.json");
        ObjectMapper objectMapper = new ObjectMapper();
        ConsumptionLoadCurveMeterReading consumptionLoadCurveMeterReading = objectMapper.readValue(content, ConsumptionLoadCurveMeterReading.class);
        assertEquals(USAGE_POINT_ID, consumptionLoadCurveMeterReading.getUsagePointId());
        assertEquals(START_DATE, consumptionLoadCurveMeterReading.getStart());
        assertEquals(END_DATE, consumptionLoadCurveMeterReading.getEnd());
        assertEquals(ConsumptionLoadCurveMeterReading.QualityEnum.BRUT, consumptionLoadCurveMeterReading.getQuality());
        assertEquals(48, consumptionLoadCurveMeterReading.getIntervalReading().size());
    }

    @Test
    void testCorrectDcMapping() throws Exception {
        String content = readClasspathResource("/daily-consumption-record.json");
        ObjectMapper objectMapper = new ObjectMapper();
        DailyConsumptionMeterReading dailyConsumptionMeterReading = objectMapper.readValue(content, DailyConsumptionMeterReading.class);
        assertEquals(USAGE_POINT_ID, dailyConsumptionMeterReading.getUsagePointId());
        assertEquals(START_DATE, dailyConsumptionMeterReading.getStart());
        assertEquals(END_DATE, dailyConsumptionMeterReading.getEnd());
        assertEquals(DailyConsumptionMeterReading.QualityEnum.BRUT, dailyConsumptionMeterReading.getQuality());
        assertEquals(DailyConsumptionReadingType.MeasuringPeriodEnum.P1D, dailyConsumptionMeterReading.getReadingType().getMeasuringPeriod());
        assertEquals(1, dailyConsumptionMeterReading.getIntervalReading().size());
    }
}
