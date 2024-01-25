package energy.eddie.regionconnector.at.eda.services;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.Energy;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ProcessDirectory;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtAcceptedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.utils.DateTimeConstants.AT_ZONE_ID;
import static org.mockito.Mockito.*;

class PermissionRequestFulfillmentServiceTest {

    @Test
    void service_callsFulfill_whenMeteringDataEndIsAfterPermissionRequestEnd() throws StateTransitionException {
        // Arrange
        ZonedDateTime permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).atStartOfDay(AT_ZONE_ID).minusDays(1);
        ZonedDateTime meteringDataEnd = permissionRequestEnd.plusDays(1);

        AtPermissionRequest permissionRequest = arrangeAndAct(permissionRequestEnd, meteringDataEnd);

        // Assert
        verify(permissionRequest).fulfill();
    }

    @Test
    void service_callsFulfill_whenMeteringDataEndIsEqualPermissionRequestEnd() throws StateTransitionException {
        // Arrange
        ZonedDateTime permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).atStartOfDay(AT_ZONE_ID);

        AtPermissionRequest permissionRequest = arrangeAndAct(permissionRequestEnd, permissionRequestEnd);

        // Assert
        verify(permissionRequest, never()).fulfill();
    }

    @Test
    void service_callsFulfill_whenMeteringDataEndIsBeforePermissionRequestEnd() throws StateTransitionException {
        // Arrange
        ZonedDateTime permissionRequestEnd = LocalDate.now(ZoneOffset.UTC).atStartOfDay(AT_ZONE_ID);
        ZonedDateTime meteringDataEnd = permissionRequestEnd.minusDays(1);

        AtPermissionRequest permissionRequest = arrangeAndAct(permissionRequestEnd, meteringDataEnd);

        // Assert
        verify(permissionRequest, never()).fulfill();
    }

    private AtPermissionRequest arrangeAndAct(ZonedDateTime permissionRequestEnd, ZonedDateTime meteringDataEnd) {
        TestPublisher<IdentifiableConsumptionRecord> testPublisher = TestPublisher.create();

        AtPermissionRequest permissionRequest = mock(AtPermissionRequest.class);
        when(permissionRequest.end()).thenReturn(permissionRequestEnd);
        when(permissionRequest.state()).thenReturn(mock(AtAcceptedPermissionRequestState.class));

        ConsumptionRecord consumptionRecord = createConsumptionRecord(meteringDataEnd);

        new PermissionRequestFulfillmentService(testPublisher.flux());

        // Act
        StepVerifier.create(testPublisher.flux())
                .then(() -> {
                    testPublisher.next(new IdentifiableConsumptionRecord(consumptionRecord, List.of(permissionRequest)));
                    testPublisher.complete();
                })
                .expectNextCount(1)
                .expectComplete()
                .verify(Duration.ofSeconds(1));
        return permissionRequest;
    }

    private ConsumptionRecord createConsumptionRecord(ZonedDateTime zonedDateTime) {
        return new ConsumptionRecord()
                .withProcessDirectory(new ProcessDirectory().withEnergy(new Energy().withMeteringPeriodEnd(DateTimeConverter.dateTimeToXml(zonedDateTime))));
    }
}