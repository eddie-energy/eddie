package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.handlers.integration.inbound.AtPermissionRequestProjectionTest;
import energy.eddie.regionconnector.at.eda.persistence.JpaPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentifiableConsumptionRecordServiceTest {

    @Mock
    private JpaPermissionRequestRepository repository;

    private static AtPermissionRequestProjection projection(String permissionId, String connectionId, String dataNeedId,
                                                            String cmRequestId, String conversationId) {
        return new AtPermissionRequestProjectionTest(
                permissionId, connectionId, cmRequestId, conversationId,
                LocalDate.now(), LocalDate.now(), dataNeedId, "dsoId", "meteringPointId", "consentId", "message",
                AllowedGranularity.PT15M.name(), "ACCEPTED", Instant.now()
        );
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void mapToIdentifiableConsumptionRecord_returnsCorrectlyMappedRecord() {

        String identifiableMeteringPoint = "identifiableMeteringPoint";
        var identifiableConsumptionRecord = createConsumptionRecord(identifiableMeteringPoint);

        var permissionRequests = List.of(
                projection("pmId1", "connId1", "dataNeedId1", "test1", "any1"),
                projection("pmId2", "connId2", "dataNeedId2", "test2", "any2")
        );

        when(repository.findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted(
                eq(identifiableMeteringPoint),
                any()
        ))
                .thenReturn(permissionRequests);

        IdentifiableConsumptionRecordService service = new IdentifiableConsumptionRecordService(repository);

        var result = service.mapToIdentifiableConsumptionRecord(identifiableConsumptionRecord);

        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(identifiableConsumptionRecord, result.get().consumptionRecord()),
                () -> assertEquals(2, result.get().permissionRequests().size())
        );
    }

    @Test
    void mapToIdentifiableConsumptionRecord_withUnmappableRecord_returnsEmpty() {
        String unidentifiableMeteringPoint = "unidentifiableMeteringPoint";
        var unidentifiableConsumptionRecord = createConsumptionRecord(unidentifiableMeteringPoint);

        when(repository.findByMeteringPointIdAndDateAndStateSentToPAOrAfterAccepted(eq(unidentifiableMeteringPoint),
                              any()))
                .thenReturn(List.of());

        IdentifiableConsumptionRecordService service = new IdentifiableConsumptionRecordService(repository);

        var result = service.mapToIdentifiableConsumptionRecord(unidentifiableConsumptionRecord);

        assertTrue(result.isEmpty());
    }

    private EdaConsumptionRecord createConsumptionRecord(String meteringPoint) {
        var meteringType = "L1";
        var consumptionValue = 10;
        var granularity = Granularity.PT15M;
        var unit = "KWH";
        return createConsumptionRecord(meteringPoint,
                                       meteringType,
                                       LocalDate.now(ZoneOffset.UTC),
                                       granularity,
                                       consumptionValue,
                                       unit);
    }

    private EdaConsumptionRecord createConsumptionRecord(
            String meteringPoint,
            String meteringType,
            LocalDate meteringPeriodStart,
            Granularity granularity,
            double consumptionValue,
            String unit
    ) {
        return new SimpleEdaConsumptionRecord()
                .setMeteringPoint(meteringPoint)
                .setStartDate(meteringPeriodStart)
                .setEndDate(meteringPeriodStart)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(granularity)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setBillingUnit(unit)
                                                .setEnergyPositions(List.of(new EnergyPosition(
                                                                            BigDecimal.valueOf(consumptionValue),
                                                                            meteringType)
                                                                    )
                                                )
                                ))
                ));
    }
}
