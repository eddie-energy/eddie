package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p31;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.Energy;
import energy.eddie.regionconnector.at.eda.dto.EnergyData;
import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MarshallerConfig.class)
class EdaConsumptionRecord01p31InboundMessageFactoryTest {

    @Autowired
    private Jaxb2Marshaller marshaller;

    private static Stream<Arguments> inputStreams() {
        ClassLoader classLoader = EdaConsumptionRecord01p31InboundMessageFactoryTest.class.getClassLoader();
        var daily = classLoader.getResourceAsStream("xsd/consumptionrecord/_01p31/consumptionrecord_daily.xml");
        var quarter_hourly = classLoader.getResourceAsStream(
                "xsd/consumptionrecord/_01p31/consumptionrecord_quater-hourly.xml");

        return Stream.of(
                Arguments.of(daily,
                             Granularity.P1D,
                             1,
                             "EPXXXXXXT1712068829927",
                             ZonedDateTime.parse("2024-04-02T14:40:48.704Z"),
                             LocalDate.of(2024, 4, 2),
                             "L1",
                             new BigDecimal("36.770000"),
                             ZonedDateTime.parse("2024-03-30T00:00:00+01:00"),
                             ZonedDateTime.parse("2024-03-31T00:00:00+01:00")),
                Arguments.of(quarter_hourly,
                             Granularity.PT15M,
                             96,
                             "EPXXXXXXT1712123513688",
                             ZonedDateTime.parse("2024-04-03T05:52:15.539Z"),
                             LocalDate.of(2024, 4, 3),
                             "L1",
                             new BigDecimal("0.001000"),
                             ZonedDateTime.parse("2024-04-01T00:00:00+02:00"),
                             ZonedDateTime.parse("2024-04-02T00:00:00+02:00"))
        );
    }


    @Test
    void isActive_on_07_04_2024_returnsTrue() {
        // given
        var factory = new EdaConsumptionRecord01p31InboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 7));

        // then
        assertTrue(active);
    }

    @Test
    void isActive_on_08_04_2024_returnsFalse() {
        // given
        var factory = new EdaConsumptionRecord01p31InboundMessageFactory(marshaller);

        // when
        var active = factory.isActive(LocalDate.of(2024, 4, 8));

        // then
        assertFalse(active);
    }

    @ParameterizedTest
    @MethodSource("inputStreams")
    void parseInputStream(
            InputStream inputStream,
            Granularity granularity,
            int expectedSize,
            String conversationId,
            ZonedDateTime documentCreationDateTime,
            LocalDate processDate,
            String firstMeteringMethod,
            BigDecimal firstMeteringValue,
            ZonedDateTime meterReadingStart,
            ZonedDateTime meterReadingEnd
    ) throws IOException {
        // Given
        var factory = new EdaConsumptionRecord01p31InboundMessageFactory(marshaller);

        // When
        var record = factory.parseInputStream(inputStream);
        inputStream.close();

        // Then
        Energy energy = record.energy().getFirst();
        EnergyData energyData = energy.energyData().getFirst();
        assertAll(
                () -> assertEquals("ATXXXXXX00000000000000000XXXXXXXX", record.meteringPoint()),
                () -> assertEquals(meterReadingStart.toLocalDate(), record.startDate()),
                () -> assertEquals(meterReadingEnd.toLocalDate(), record.endDate()),
                () -> assertNotNull(record.originalConsumptionRecord()),
                () -> assertEquals(documentCreationDateTime,
                                   record.documentCreationDateTime()
                                         .withZoneSameInstant(documentCreationDateTime.getZone())),
                () -> assertEquals("01.31", record.schemaVersion()),
                () -> assertEquals("ATXXXXXX", record.senderMessageAddress()),
                () -> assertEquals("EPXXXXXX", record.receiverMessageAddress()),
                () -> assertEquals(conversationId, record.conversationId()),
                () -> assertEquals(processDate.toString(), record.processDate().toString()),
                () -> assertEquals(1, record.energy().size()),
                () -> assertEquals(granularity, energy.granularity()),
                () -> assertEquals("00", energy.meteringReason()),
                () -> assertEquals(meterReadingStart,
                                   energy.meterReadingStart().withZoneSameInstant(meterReadingStart.getZone())),
                () -> assertEquals(meterReadingEnd,
                                   energy.meterReadingEnd().withZoneSameInstant(meterReadingEnd.getZone())),
                () -> assertEquals(1, energy.energyData().size()),
                () -> assertEquals("1-1:2.9.0 P.01", energyData.meterCode()),
                () -> assertEquals("KWH", energyData.billingUnit()),
                () -> assertEquals(expectedSize, energyData.energyPositions().size()),
                () -> assertEquals(firstMeteringMethod, energyData.energyPositions().getFirst().meteringMethod()),
                () -> assertEquals(firstMeteringValue, energyData.energyPositions().getFirst().billingQuantity())
        );
    }
}
