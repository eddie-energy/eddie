package energy.eddie.outbound.shared.utils;

import energy.eddie.cim.v0_91_08.ESMPDateTimeInterval;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RetransmissionRequestMapperTest {

    @Test
    void testToRetransmissionRequest_correctlyMaps() {
        // Given
        var dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var envelope = new RTREnvelope()
                .withMarketDocumentMRID("mrid")
                .withMessageDocumentHeaderCreationDateTime(dateTime)
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMessageDocumentHeaderMetaInformationRegionConnector("rc-id")
                .withMarketDocumentPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart("2024-01-01T00:00Z")
                                .withEnd("2024-01-02T00:00Z")
                );
        var mapper = new RetransmissionRequestMapper(envelope);

        // When
        var res = mapper.toRetransmissionRequest();

        // Then
        assertAll(
                () -> assertEquals("pid", res.permissionId()),
                () -> assertEquals("rc-id", res.regionConnectorId()),
                () -> assertEquals(LocalDate.of(2024, 1, 1), res.from()),
                () -> assertEquals(LocalDate.of(2024, 1, 2), res.to())
        );
    }
}