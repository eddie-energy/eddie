// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_12;

import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.ValidatedHistoricalDataVersionComparator;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.cds.providers.vhd.v1_04.CdsValidatedHistoricalDataMarketDocumentV104Provider;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class CdsValidatedHistoricalDataMarketDocumentV112ProviderTest {
    private static final OffsetDateTime NOW = OffsetDateTime.now(ZoneOffset.UTC);
    private final XmlMessageSerde serde = new XmlMessageSerde();

    CdsValidatedHistoricalDataMarketDocumentV112ProviderTest() throws SerdeInitializationException {}

    @Test
    void testValidatedHistoricalDataMarketDocument_returnsDocuments() {
        // Given
        var streams = new IdentifiableDataStreams();
        var provider = new CdsValidatedHistoricalDataMarketDocumentV112Provider(streams);

        // When & Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(() -> publish(streams))
                    .then(streams::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testValidatedHistoricalDataMarketDocument_isEquivalentToV104AndValid() throws ExecutionException, InterruptedException, SerializationException {
        // Given
        var streams = new IdentifiableDataStreams();
        var v104Future = new CdsValidatedHistoricalDataMarketDocumentV104Provider(streams)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();
        var v112Future = new CdsValidatedHistoricalDataMarketDocumentV112Provider(streams)
                .getValidatedHistoricalDataMarketDocumentsStream()
                .collectList()
                .toFuture();

        // When
        publish(streams);
        streams.close();

        // Then
        var v104 = v104Future.get();
        var v112 = v112Future.get();
        assertNotNull(v104);
        assertNotNull(v112);
        assertEquals(1, v104.size());
        assertEquals(1, v112.size());
        // The creation time is taken from the clock at mapping time, so it differs between versions
        assertEquals(List.of(),
                     ValidatedHistoricalDataVersionComparator.differencesIgnoring(v104.getFirst(),
                                                                                  v112.getFirst(),
                                                                                  "createdDateTime"));
        var envelope = v112.getFirst();
        assertEquals("US", envelope.getMessageDocumentHeader().getMetaInformation().getRegionCountry());
        var vhd = envelope.getVHDMarketDocument();
        vhd.getTimeSeries().forEach(ts -> assertEquals(vhd.getCreatedDateTime(), ts.getDateAndOrTimeDateTime()));
        var xml = serde.serialize(envelope);
        assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(xml),
                   "Failed to validate XML, see:\n" + new String(xml, StandardCharsets.UTF_8));
    }

    private static void publish(IdentifiableDataStreams streams) {
        var account = new AccountsEndpoint200ResponseAllOfAccountsInner()
                .customerNumber("customer-number")
                .cdsAccountId("cds-account-id");
        var serviceContract = new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()
                .cdsAccountId("cds-account-id")
                .cdsServicecontractId("cds-servicecontract-id");
        var servicePoint = new ServicePointEndpoint200ResponseAllOfServicePointsInner()
                .cdsServicepointId("cds-servicepoint-id")
                .currentServicecontracts(List.of("cds-servicecontract-id"));
        var meterDevice = new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()
                .cdsMeterdeviceId("cds-meterdevice-id")
                .meterNumber("meter-number")
                .addCurrentServicepointsItem("cds-servicepoint-id");
        var usageSegment = new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                .addRelatedMeterdevicesItem("cds-meterdevice-id")
                .segmentStart(NOW)
                .segmentEnd(NOW)
                .interval(BigDecimal.valueOf(900))
                .format(List.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_FWD,
                                UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_REV,
                                UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_NET))
                .values(List.of(
                        List.of(Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE)),
                        List.of(Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE))
                ));
        streams.publishValidatedHistoricalData(
                permissionRequest(),
                List.of(account),
                List.of(serviceContract),
                List.of(servicePoint),
                List.of(meterDevice),
                List.of(usageSegment)
        );
    }

    private static CdsPermissionRequest permissionRequest() {
        return new CdsPermissionRequestBuilder()
                .setCdsServer(1)
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .setConnectionId("cid")
                .setDataStart(NOW.toLocalDate())
                .setDataEnd(NOW.toLocalDate())
                .build();
    }
}
