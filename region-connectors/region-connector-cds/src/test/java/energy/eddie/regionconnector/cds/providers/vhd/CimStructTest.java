package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class CimStructTest {

    @Test
    void testGet_returnsAccounts() {
        // Given
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
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var usageSegment = new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                .addRelatedMeterdevicesItem("cds-meterdevice-id")
                .segmentStart(now)
                .segmentEnd(now)
                .interval(BigDecimal.valueOf(900))
                .format(List.of(FormatEnum.KWH_FWD,
                                FormatEnum.KWH_REV,
                                FormatEnum.KWH_NET))
                .values(List.of(
                        List.of(Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE)),
                        List.of(Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE), Map.of("v", BigDecimal.ONE))
                ));
        var id = new IdentifiableValidatedHistoricalData(
                new CdsPermissionRequestBuilder().build(),
                new IdentifiableValidatedHistoricalData.Payload(
                        List.of(account),
                        List.of(serviceContract),
                        List.of(servicePoint),
                        List.of(meterDevice),
                        List.of(usageSegment)
                )
        );
        var struct = new CimStruct(id);

        // When
        var res = struct.get();

        // Then
        assertThat(res)
                .singleElement()
                .satisfies(acc -> {
                               assertThat(acc.cdsCustomerNumber()).isEqualTo("customer-number");
                               assertThat(acc.meters())
                                       .singleElement()
                                       .satisfies(meter -> {
                                           assertThat(meter.meterNumber()).isEqualTo("meter-number");
                                           assertThat(meter.usageSegments())
                                                   .singleElement()
                                                   .satisfies(segment -> {
                                                       assertThat(segment.start()).isEqualTo(now.toZonedDateTime());
                                                       assertThat(segment.end()).isEqualTo(now.toZonedDateTime());
                                                       assertThat(segment.interval()).isEqualTo(BigDecimal.valueOf(900));
                                                       assertThat(segment.usageSegmentValues())
                                                               .containsOnly(
                                                                       entry(FormatEnum.KWH_FWD,
                                                                             List.of(BigDecimal.ONE, BigDecimal.ONE)),
                                                                       entry(FormatEnum.KWH_REV,
                                                                             List.of(BigDecimal.ONE, BigDecimal.ONE)),
                                                                       entry(FormatEnum.KWH_NET,
                                                                             List.of(BigDecimal.ONE, BigDecimal.ONE))
                                                               );
                                                   });
                                       });
                           }
                );
    }
}